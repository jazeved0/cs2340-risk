@echo off
title production-script

REM command prefixes
set script_prefix=[92m[Docker Push Script][0m
set intermediate_prefix=[93m[Docker Push Script] -[0m
set finished_prefix=[94m[Docker Push Script][0m

REM parameters
set local_image=%1
set remote_image=%2
set registry=%3

echo %script_prefix% Tagging local image to remote
if "%registry%"=="" (
  call docker tag %local_image% %remote_image%
  echo %intermediate_prefix% Tagged %local_image% with %remote_image%
  echo %script_prefix% Pushing local image to remote
  call docker push %remote_image%
) else (
  call docker tag %local_image% %registry%/%remote_image%
  echo %intermediate_prefix% Tagged %local_image% with %registry%/%remote_image%
  echo %script_prefix% Pushing local image to remote
  call docker push %registry%/%remote_image%
)

echo %finished_prefix% Finished
