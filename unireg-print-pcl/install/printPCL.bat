set PRINT_PCL_LIBS=C:\Program Files\PrintPCL\uniregctb-print-pcl-1.0.jar;C:\Program Files\PrintPCL\log4j-1.2.14.jar
set CLASSPATH=%PRINT_PCL_LIBS%;%CLASSPATH%

start javaw ch.vd.uniregctb.PrintPCL %1

