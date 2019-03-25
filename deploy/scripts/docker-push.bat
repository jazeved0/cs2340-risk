@echo off
title production-script

REM command prefixes
set script_prefix=[92m[Docker Push Script][0m
set intermediate_prefix=[93m[Docker Push Script] -[0m
set finished_prefix=[94m[Docker Push Script][0m

REM parameters
set local_image=risk-main
set registry=riskreg.azurecr.io
set remote_image=risk-main
set tag=latest

echo %script_prefix% Tagging local image to remote
call docker tag %local_image% %registry%/%remote_image%:%tag%
echo %intermediate_prefix% Tagged %local_image% with %registry%/%remote_image%:%tag%

echo %script_prefix% Pushing local image to remote
call docker push %registry%/%remote_image%:%tag%

echo %finished_prefix% Finished
