/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adlinktech.gateway.camelext;

import static org.apache.camel.util.ExchangeHelper.copyResultsPreservePattern;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelExchangeException;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.impl.BridgeExceptionHandlerToErrorHandler;
import org.apache.camel.impl.ConsumerCache;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.EmptyConsumerCache;
import org.apache.camel.impl.EventDrivenPollingConsumer;
import org.apache.camel.model.PollEnrichDefinition;
import org.apache.camel.processor.PollEnricher;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.spi.IdAware;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.ServiceHelper;
import org.slf4j.Logger;

import com.adlinktech.gateway.camelospl.utils.Loggers;


/**
 * This class is a Processor acting as a "dynamic" version of the {@link PollEnricher} class,
 * managing a {@link DynamicPollingConsumer} instead of a {@link PollingConsumer}.
 * In {@link DynamicPollEnricher#process(Exchange)} operation, the headers of IN message are used
 * as options to call {@link DynamicPollingConsumer#receive(java.util.Map)} operation.
 * Note that this class has to be used as a Processor, since the Camel DSL is not extensible (i.e.
 * it won't instantiate this class). Moreover, this prevent this class to inherit from
 * {@link PollEnricher} because Camel would require an associated {@link PollEnrichDefinition} that
 * we cannot extend to
 * instantiate a {@link DynamicPollEnricher}.
 */
public class DynamicPollEnricher
   extends ServiceSupport
   implements AsyncProcessor, CamelContextAware, IdAware
{

   // the delegate (because this class can't inherit directly from PollEnricher)
   private DynamicPollEnricherDelegate delegate;

   public DynamicPollEnricher(String uri)
   {
      delegate = new DynamicPollEnricherDelegate(uri);
   }

   public DynamicPollEnricher(String uri, long timeout)
   {
      delegate = new DynamicPollEnricherDelegate(uri, timeout);
   }

   public void setAggregationStrategy(AggregationStrategy strategy)
   {
      delegate.setAggregationStrategy(strategy);
   }

   public AggregationStrategy getAggregationStrategy()
   {
      return delegate.getAggregationStrategy();
   }

   @Override
   public void process(Exchange exchange) throws Exception
   {
      delegate.process(exchange);

   }

   @Override
   public boolean process(Exchange exchange, AsyncCallback callback)
   {
      return delegate.process(exchange, callback);
   }

   @Override
   protected void doStart() throws Exception
   {
      delegate.doStart();
   }

   @Override
   protected void doStop() throws Exception
   {
      delegate.doStop();
   }

   @Override
   public String getId()
   {
      return delegate.getId();
   }

   @Override
   public void setId(String id)
   {
      delegate.setId(id);

   }

   @Override
   public void setCamelContext(CamelContext camelContext)
   {
      delegate.setCamelContext(camelContext);
   }

   @Override
   public CamelContext getCamelContext()
   {
      return delegate.getCamelContext();
   }
}


/**
 * This class extends the PollEnricher class and is the concrete implementation.
 */
class DynamicPollEnricherDelegate
   extends PollEnricher
{

   private static final Logger LOG = Loggers.LOGGER_GENERAL;

   /* To remove if no longer private in parent class */
   private ConsumerCache consumerCache;

   public DynamicPollEnricherDelegate(String uri)
   {
      this(uri, 0);
   }

   public DynamicPollEnricherDelegate(String uri, long timeout)
   {
      super(new SimpleBuilder(uri), timeout);
      setAggregationStrategy(defaultAggregationStrategy());
   }


   @Override
   public boolean process(Exchange exchange, AsyncCallback callback)
   {
      try
      {
         preCheckPoll(exchange);
      }
      catch (Exception e)
      {
         exchange.setException(new CamelExchangeException("Error during pre poll check", exchange,
               e));
         callback.done(true);
         return true;
      }

      // which consumer to use
      PollingConsumer consumer;
      Endpoint endpoint;

      // use dynamic endpoint so calculate the endpoint to use
      Object recipient = null;
      try
      {
         recipient = getExpression().evaluate(exchange, Object.class);
         endpoint = resolveEndpoint(exchange, recipient);
         // acquire the consumer from the cache
         consumer = consumerCache.acquirePollingConsumer(endpoint);
      }
      catch (Throwable e)
      {
         if (isIgnoreInvalidEndpoint())
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Endpoint uri is invalid: " + recipient
                     + ". This exception will be ignored.", e);
            }
         }
         else
         {
            exchange.setException(e);
         }
         callback.done(true);
         return true;
      }

      // grab the real delegate consumer that performs the actual polling
      Consumer delegate = consumer;
      if (consumer instanceof EventDrivenPollingConsumer)
      {
         delegate = ((EventDrivenPollingConsumer) consumer).getDelegateConsumer();
      }

      // is the consumer bridging the error handler?
      boolean bridgeErrorHandler = false;
      if (delegate instanceof DefaultConsumer)
      {
         ExceptionHandler handler = ((DefaultConsumer) delegate).getExceptionHandler();
         if (handler != null && handler instanceof BridgeExceptionHandlerToErrorHandler)
         {
            bridgeErrorHandler = true;
         }
      }

      Exchange resourceExchange;
      try
      {
         resourceExchange = doPolling(consumer, exchange);
      }
      catch (Exception e)
      {
         exchange.setException(new CamelExchangeException("Error during poll", exchange, e));
         callback.done(true);
         return true;
      }
      finally
      {
         // return the consumer back to the cache
         consumerCache.releasePollingConsumer(endpoint, consumer);
      }

      // remember current redelivery stats
      Object redeliveried = exchange.getIn().getHeader(Exchange.REDELIVERED);
      Object redeliveryCounter = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
      Object redeliveryMaxCounter = exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);

      // if we are bridging error handler and failed then remember the caused exception
      Throwable cause = null;
      if (resourceExchange != null && bridgeErrorHandler)
      {
         cause = resourceExchange.getException();
      }

      try
      {
         if ( !isAggregateOnException()
               && (resourceExchange != null && resourceExchange.isFailed()))
         {
            // copy resource exchange onto original exchange (preserving pattern)
            // and preserve redelivery headers
            copyResultsPreservePattern(exchange, resourceExchange);
         }
         else
         {
            prepareResult(exchange);

            // prepare the exchanges for aggregation
            ExchangeHelper.prepareAggregation(exchange, resourceExchange);
            // must catch any exception from aggregation
            Exchange aggregatedExchange = getAggregationStrategy().aggregate(exchange,
                  resourceExchange);
            if (aggregatedExchange != null)
            {
               // copy aggregation result onto original exchange (preserving pattern)
               copyResultsPreservePattern(exchange, aggregatedExchange);
               // handover any synchronization
               if (resourceExchange != null)
               {
                  resourceExchange.handoverCompletions(exchange);
               }
            }
         }

         // if we failed then restore caused exception
         if (cause != null)
         {
            // restore caused exception
            exchange.setException(cause);
            // remove the exhausted marker as we want to be able to perform redeliveries with the
            // error handler
            exchange.removeProperties(Exchange.REDELIVERY_EXHAUSTED);

            // preserve the redelivery stats
            if (redeliveried != null)
            {
               if (exchange.hasOut())
               {
                  exchange.getOut().setHeader(Exchange.REDELIVERED, redeliveried);
               }
               else
               {
                  exchange.getIn().setHeader(Exchange.REDELIVERED, redeliveried);
               }
            }
            if (redeliveryCounter != null)
            {
               if (exchange.hasOut())
               {
                  exchange.getOut().setHeader(Exchange.REDELIVERY_COUNTER, redeliveryCounter);
               }
               else
               {
                  exchange.getIn().setHeader(Exchange.REDELIVERY_COUNTER, redeliveryCounter);
               }
            }
            if (redeliveryMaxCounter != null)
            {
               if (exchange.hasOut())
               {
                  exchange.getOut()
                        .setHeader(Exchange.REDELIVERY_MAX_COUNTER, redeliveryMaxCounter);
               }
               else
               {
                  exchange.getIn().setHeader(Exchange.REDELIVERY_MAX_COUNTER, redeliveryMaxCounter);
               }
            }
         }

         // set header with the uri of the endpoint enriched so we can use that for tracing etc
         if (exchange.hasOut())
         {
            exchange.getOut().setHeader(Exchange.TO_ENDPOINT,
                  consumer.getEndpoint().getEndpointUri());
         }
         else
         {
            exchange.getIn().setHeader(Exchange.TO_ENDPOINT,
                  consumer.getEndpoint().getEndpointUri());
         }
      }
      catch (Throwable e)
      {
         exchange.setException(new CamelExchangeException("Error occurred during aggregation",
               exchange, e));
         callback.done(true);
         return true;
      }

      callback.done(true);
      return true;
   }

   private Exchange doPolling(PollingConsumer consumer, Exchange exchange)
   {
      Exchange resourceExchange;
      if (consumer instanceof DynamicPollingConsumer)
      {
         DynamicPollingConsumer dynConsumer = (DynamicPollingConsumer) consumer;
         if (getTimeout() < 0)
         {
            LOG.debug("Consumer receive: {}", dynConsumer);
            resourceExchange = dynConsumer.receive(exchange.getIn().getHeaders());
         }
         else if (getTimeout() == 0)
         {
            LOG.debug("Consumer receiveNoWait: {}", dynConsumer);
            resourceExchange = dynConsumer.receiveNoWait(exchange.getIn().getHeaders());
         }
         else
         {
            LOG.debug("Consumer receive with timeout: {} ms. {}", getTimeout(), dynConsumer);
            resourceExchange = dynConsumer.receive(exchange.getIn().getHeaders(), getTimeout());
         }

         if (resourceExchange == null)
         {
            LOG.debug("Consumer received no exchange");
         }
         else
         {
            LOG.debug("Consumer received: {}", resourceExchange);
         }
      }
      else
      {
         if (getTimeout() < 0)
         {
            LOG.debug("Consumer receive: {}", consumer);
            resourceExchange = consumer.receive();
         }
         else if (getTimeout() == 0)
         {
            LOG.debug("Consumer receiveNoWait: {}", consumer);
            resourceExchange = consumer.receiveNoWait();
         }
         else
         {
            LOG.debug("Consumer receive with timeout: {} ms. {}", getTimeout(), consumer);
            resourceExchange = consumer.receive(getTimeout());
         }

         if (resourceExchange == null)
         {
            LOG.debug("Consumer received no exchange");
         }
         else
         {
            LOG.debug("Consumer received: {}", resourceExchange);
         }
      }
      return resourceExchange;
   }

   @Override
   public String toString()
   {
      return "DynamicPollEnricher[" + getExpression() + "]";
   }

   @Override
   protected void doStart() throws Exception
   {
      if (consumerCache == null)
      {
         // create consumer cache if we use dynamic expressions for computing the endpoints to poll
         if (getCacheSize() < 0)
         {
            consumerCache = new EmptyConsumerCache(this, getCamelContext());
            LOG.debug("PollEnrich {} is not using ConsumerCache", this);
         }
         else if (getCacheSize() == 0)
         {
            consumerCache = new ConsumerCache(this, getCamelContext());
            LOG.debug("PollEnrich {} using ConsumerCache with default cache size", this);
         }
         else
         {
            consumerCache = new ConsumerCache(this, getCamelContext(), getCacheSize());
            LOG.debug("PollEnrich {} using ConsumerCache with cacheSize={}", this, getCacheSize());
         }
      }
      ServiceHelper.startServices(consumerCache, getAggregationStrategy());
   }

   @Override
   protected void doStop() throws Exception
   {
      ServiceHelper.stopServices(getAggregationStrategy(), consumerCache);
   }

   @Override
   protected void doShutdown() throws Exception
   {
      ServiceHelper.stopAndShutdownServices(getAggregationStrategy(), consumerCache);
   }


   /* To remove if no longer private in parent class */
   private static void prepareResult(Exchange exchange)
   {
      if (exchange.getPattern().isOutCapable())
      {
         exchange.getOut().copyFrom(exchange.getIn());
      }
   }

   /* To remove if no longer private in parent class */
   private static AggregationStrategy defaultAggregationStrategy()
   {
      return new CopyAggregationStrategy();
   }

   /* To remove if no longer private in parent class */
   private static class CopyAggregationStrategy
      implements AggregationStrategy
   {

      public Exchange aggregate(Exchange oldExchange, Exchange newExchange)
      {
         if (newExchange != null)
         {
            copyResultsPreservePattern(oldExchange, newExchange);
         }
         return oldExchange;
      }
   }


}
