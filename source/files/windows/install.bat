DicomRouter.exe ^
 //IS//DicomRouter ^
 --Install="${home}"\windows\DicomRouter.exe ^
 --Description="DicomRouter Service" ^
 --Startup="auto" ^
 --Jvm=auto ^
 --StartMode=jvm ^
 --JvmMs=128 ^
 --JvmMx=512 ^
 --Classpath="DicomRouter.jar" ^
 --StartPath="${home}" ^
 --StartClass=org.rsna.router.DicomRouter ^
 --StartMethod=startService ^
 --StartParams=start ^
 --StopMode=jvm ^
 --StopPath="${home}" ^
 --StopClass=org.rsna.router.DicomRouter ^
 --StopMethod=stopService ^
 --StopParams=stop ^
 --LogPath="${home}"\logs ^
 --StdOutput=auto ^
 --StdError=auto
