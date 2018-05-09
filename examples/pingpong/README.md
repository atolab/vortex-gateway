# pingpong

 Pingpong example consists of two executables that will exchange data using 2 partitions.
 Running both executables allows to measure roundtrip duration when sending and
 receiving back a single message.


## Building the example
 From the pingpong directory run the following command: 
 
 `mvn package`


## Running the example

 - Open a terminal and change directory to "pingpong". Enter the following command:
   
   `mvn -Ppong exec:java`

 - Open another terminal in "pingpong" directory and enter the following command:
   
   `mvn -Pping exec:java`


## Ping command options

 The ping command accepts some options to be passed via the "exec.args" property:
 
 `mvn -Pping exec:java -Dexec.args="blocks nof_cycles topic_id READ_PARTITION WRITE_PARTITION"`

 Where:
   * `blocks`:     number of roundtrips in each statistics calculation
   * `nof_cycles`: how many times such a statistics calculation is run
   * `topic_id`:   for the topic, there's a choice between several 
                   pre-configured topics.
                   topic_id allows selection of topic used for the test, among those
                   defined by pragma keylist in pinpong.idl, and may be one of : 
      - `m` (PP_min_msg),
      - `q` (PP_seq_msg), 
      - `s` (PP_string_msg), 
      - `f` (PP_fixed_msg), 
      - `a` (PP_array_msg), 
      - `t` (PP_quit_msg)
                
   * `READ_PARTITION` and `WRITE_PARTITION`: 
         this enables to use several PING-PONG pairs simultaneous with 
         them interfering with each other. It also enables creating
         larger loops, by chaining several PONG tests to one PING test.
         By default, the values are "PING" and "PONG"

 Example of command to start 10 blocks of 100 cycles on PP_string_msg topic:
 
 `mvn -Pping exec:java -Dexec.args="10 100 s PING PONG"`
   

## Compatibility with OpenSpliceDDS' pingpong example
 This example is fully compatible with OpenSpliceDDS' pingpong example.
 You can use this example's "ping" command with OpenSpliceDDS' "pong" command,
 or the OpenSpliceDDS' "ping" command with this example's "pong" command.


