#!/usr/bin/env bash
BIN_DIR=`pwd`
cd ..
APP_DIR=`pwd`
echo 'APP_DIR:'${APP_DIR}

LOG_DIR=${APP_DIR}/log
echo 'LOG_DIR:'${LOG_DIR}

rm -f ${LOG_DIR}/stdout.log
mkdir ${LOG_DIR}/
touch ${LOG_DIR}/stdout.log