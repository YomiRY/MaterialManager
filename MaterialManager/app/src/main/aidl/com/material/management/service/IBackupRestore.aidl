package com.material.management.service;
import com.material.management.IStatusUpdate;

interface IBackupRestore {  
  void startBackup(IStatusUpdate statusRemoteHandler);
  void startRestore(IStatusUpdate statusRemoteHandler);
  void connect();
  void disConnect();
  boolean isLinked();
   
}