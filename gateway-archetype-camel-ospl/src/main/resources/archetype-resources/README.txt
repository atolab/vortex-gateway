Welcome to your Vortex Gateway project
===========================================

To create your first Gateway project, proceed as following:

1) Copy or create your IDL files in src/main/idl/ directory

2) Complete the file src/main/java/<your_package_path>/GatewayRoutesDefinition.java
   with your own Camel routes definition.
   
3) Compile your Gateway project:
     mvn package
     
4) Run your Gateway project:
     mvn exec:java
   (don't forget to start OpenSpliceDDS daemon at first, if not in SingleProcess mode)
   
 


