@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem @rem
@rem @rem  Gradle startup script for Windows
@rem @rem
@rem @rem ##########################################################################
@rem
@rem @rem Set local scope for the variables with windows NT shell
@rem if "%OS%"=="Windows_NT" setlocal
@rem
@rem @rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
@rem set DEFAULT_JVM_OPTS=
@rem
@rem set DIRNAME=%~dp0
@rem if "%DIRNAME%" == "" set DIRNAME=.
@rem set APP_BASE_NAME=%~n0
@rem set APP_HOME=%DIRNAME%
@rem
@rem @rem Find java.exe
@rem if defined JAVA_HOME goto findJavaFromJavaHome
@rem
@rem set JAVA_EXE=java.exe
@rem %JAVA_EXE% -version >NUL 2>&1
@rem if "%ERRORLEVEL%" == "0" goto init
@rem
@rem echo.
@rem echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
@rem echo.
@rem echo Please set the JAVA_HOME variable in your environment to match the
@rem echo location of your Java installation.
@rem
@rem goto fail
@rem
@rem :findJavaFromJavaHome
@rem set JAVA_HOME=%JAVA_HOME:"=%
@rem set JAVA_EXE=%JAVA_HOME%/bin/java.exe
@rem
@rem if exist "%JAVA_EXE%" goto init
@rem
@rem echo.
@rem echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
@rem echo.
@rem echo Please set the JAVA_HOME variable in your environment to match the
@rem echo location of your Java installation.
@rem
@rem goto fail
@rem
@rem :init
@rem @rem Get command-line arguments, handling Windowz variants
@rem
@rem if not "%OS%" == "Windows_NT" goto win9xME_args
@rem if "%@eval[2+2]" == "4" goto 4NT_args
@rem
@rem :win9xME_args
@rem @rem Slurp the command line arguments.
@rem set CMD_LINE_ARGS=
@rem set _SKIP=2
@rem
@rem :win9xME_args_slurp
@rem if "x%~1" == "x" goto execute
@rem
@rem set CMD_LINE_ARGS=%*
@rem goto execute
@rem
@rem :4NT_args
@rem @rem Get arguments from the 4NT Shell from JP Software
@rem set CMD_LINE_ARGS=%$
@rem
@rem :execute
@rem @rem Setup the command line
@rem
@rem set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
@rem
@rem @rem Execute Gradle
@rem "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
@rem
@rem :end
@rem @rem End local scope for the variables with windows NT shell
@rem if "%ERRORLEVEL%"=="0" goto mainEnd
@rem
@rem :fail
@rem rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
@rem rem the _cmd.exe /c_ return code!
@rem if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
@rem exit /b 1
@rem
@rem :mainEnd
@rem if "%OS%"=="Windows_NT" endlocal
@rem
@rem :omega
