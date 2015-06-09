package com.material.management;
import com.material.management.data.BackupRestoreInfo;

interface IStatusUpdate {
  
  void updateProgress(String msg, int progress);  
  void finishBackupOrRestore(String msg);  
}