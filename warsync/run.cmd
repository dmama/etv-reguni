
set CP=lib/jnotify-0.91.jar;lib/log4j-1.2.14.jar;target/warsync-1.0-SNAPSHOT.jar
java -Djava.library.path=lib -cp %CP% ch.vd.warsync.WarSyncMain
