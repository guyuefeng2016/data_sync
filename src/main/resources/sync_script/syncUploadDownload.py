# coding: utf-8
# !/usr/bin/python
# author: huwenfeng

import sched
import time
from datetime import datetime
import os
import sys
from ftplib import FTP
import getopt
from  pyinotify import  WatchManager, Notifier, \
ProcessEvent,IN_DELETE, IN_CREATE,IN_MODIFY
import pyinotify

#触发类型，批量读取列表文件还是增量读取新增文件，注：批量读取文件列表会删除源文件进行上传
actionType = 'batchRead'
# 当前操作是上传还是下载
execType = ''
ftpHostIp = ''
ftpAccount = ''
ftpPassword = ''
# 所有配置的目录，末尾必须加上 // 或者 / 分隔符
# 监控的本地目录,上传文件的目录
ftpMonitorLocalSyncDir = ''
# ftp上传的远程目录
ftpUploadRemoteDir = ''
# ftp下载的远程目录
ftpDownloadRemoteDir = ''
# ftp下载之后存放的本地目录
ftpDownloadLocalDir = ''
# 多少秒执行一次
execSeconds = 300

# init sched module
schedule = sched.scheduler(time.time, time.sleep)

class FTPConnect:
    def __init__(self, ftpHostIp, ftpAccount, ftpPassword):
        self.ftpHostIp = ftpHostIp
        self.ftpAccount = ftpAccount
        self.ftpPassword = ftpPassword

        self.canDownLoad = True
        self.canUpload = True
        # init connect
        self.ftpconnect()

    # connect ftp
    def ftpconnect(self):
        try:
            self.ftp = FTP()
            # self.ftp.set_debuglevel(2)
            self.ftp.connect(self.ftpHostIp, 21)
            self.ftp.login(self.ftpAccount, self.ftpPassword)
        except Exception as e:
            raise RuntimeError('ftp连接错误, %s ' % e)

    # download from ftp
    def downloadfile(self):
        if self.canDownLoad == False:
            return
        self.canDownLoad = False
        self.checkFtpConnection()
        bufsize = 1024
        try:
            # get remote file path
            fileArr = self.ftp.nlst(ftpDownloadRemoteDir)
            if len(fileArr) == 0:
                return
            fileArr.sort()
            for f in fileArr:
                try:
                    fileName = f[f.rindex('/') + 1:]
                    fp = open(ftpDownloadLocalDir + fileName, 'wb')
                    remoteFile = f
                    self.ftp.retrbinary('RETR ' + remoteFile, fp.write, bufsize)
                    self.ftp.set_debuglevel(0)
                    self.ftp.delete(remoteFile)
                except Exception as e:
                    print('下载文件 %s 发生异常了 %s' % (f, e))
                finally:
                    fp.close()
        except Exception as e:
            self.canDownLoad = True
            print('下载发生了异常了, %s' % e)
        finally:
            self.canDownLoad = True


    # upload file to ftp
    def uploadfile(self, fileName):
        if self.canUpload == False:
            return
        self.canUpload = False
        self.checkFtpConnection()
        bufsize = 1024
        try:
            localUploadFile = ftpMonitorLocalSyncDir + fileName
            fp = open(localUploadFile, 'rb')
            self.ftp.storbinary('STOR ' + ftpUploadRemoteDir + fileName, fp, bufsize)
            self.ftp.set_debuglevel(0)
            os.remove(localUploadFile)
        except Exception as e:
            print('发生了异常了, %s' % e)
        finally:
            self.canUpload = True
            fp.close()

    #monitor file upload to ftp
    def monitorUploadfile(self, filePath):
        if self.canUpload == False:
            return
        self.canUpload = False
        self.checkFtpConnection()
        bufsize = 1024
        try:
            fileName = filePath[filePath.rindex('/'):]
            fp = open(filePath, 'rb')
            self.ftp.storbinary('STOR ' + ftpUploadRemoteDir + fileName, fp, bufsize)
            self.ftp.set_debuglevel(0)
        except Exception as e:
            print('发生了异常了, %s' % e)
        finally:
            self.canUpload = True
            fp.close()


    def checkFtpDir(self):
        try:
            if execType == 'upload':
               self.ftp.dir(ftpUploadRemoteDir)
        except Exception as e:
            raise RuntimeError("ftp上传的远程目录不存在，请提前创建！！！！")
        try:
            if execType == 'download':
               self.ftp.dir(ftpDownloadRemoteDir)
        except Exception as e:
            raise RuntimeError("ftp下载的远程目录不存在，请提前创建！！！！")


    # check ftp connection
    def checkFtpConnection(self):
        try:
            self.ftp.pwd()
        except Exception as e:
            print("ftp连接已断开，正在自动重连...")
        finally:
            self.ftpconnect()


# monitor sync local dir to upload
def monitorSyncLocalUploadDir(ftpConnect):
    if ftpMonitorLocalSyncDir == '':
        print('请输入python监控的本地目录，准备上传到ftp,  [ require -f <ftpMonitorLocalSyncDir> ]')
        sys.exit(0)
    if ftpUploadRemoteDir == '':
        print('请输入上传ftp的远程目录,  [ require -f <ftpUploadRemoteDir> ]')
        sys.exit(0)
    try:
        fileArr = os.listdir(ftpMonitorLocalSyncDir)
    except Exception as e:
        raise RuntimeError('上传文件的本地目录有问题，%s' % e)
    if len(fileArr) == 0:
        return
    fileArr.sort()
    for f in fileArr:
        try:
            print('文件 %s 开始上传...' % f)
            ftpConnect.uploadfile(str(f))
        except Exception as e:
            print('上传发生异常了, %s' % e)


# download ftp file
def downloadFileFromFTP(ftpConnect):
    if ftpDownloadLocalDir == '':
        print('请输入从ftp上下载文件到本地目录,  < ftpDownloadLocalDir >')
        sys.exit(0)
    if ftpDownloadRemoteDir == '':
        print('请输入从ftp上下载的远程目录,  < ftpDownloadRemoteDir >')
        sys.exit(0)
    ftpConnect.downloadfile()


# execute regular
def execRegular(ftpConnect, inc):
    if execType == 'upload':
        monitorSyncLocalUploadDir(ftpConnect)
        schedule.enter(inc, 0, execRegular, (ftpConnect, inc))
    elif execType == 'download':
        downloadFileFromFTP(ftpConnect)
        schedule.enter(inc, 0, execRegular, (ftpConnect, inc))


def runSchedule(inc):
    print("开始进入定时调度执行任务...")
    ftpConnect = FTPConnect(ftpHostIp, ftpAccount, ftpPassword)
    schedule.enter(0, 0, execRegular, (ftpConnect, inc))
    schedule.run()


#handle event
class EventHandler(ProcessEvent):

    def __init__(self):
        self.ftpConnect = FTPConnect(ftpHostIp, ftpAccount, ftpPassword)

    def process_IN_CREATE(self, event):
        if ftpMonitorLocalSyncDir == '':
            print('请输入python监控的本地目录，准备上传到ftp,  [ require -f <ftpMonitorLocalSyncDir> ]')
            sys.exit(0)
        if ftpUploadRemoteDir == '':
            print('请输入上传ftp的远程目录,  [ require -f <ftpUploadRemoteDir> ]')
            sys.exit(0)
        filePath = os.path.join(event.path, event.name)
        self.ftpConnect.monitorUploadfile(filePath)

    def process_IN_DELETE(self, event):
        pass
    def process_IN_MODIFY(self, event):
        pass

def monitorDir():
    wm = WatchManager()
    mask = IN_DELETE | IN_CREATE | IN_MODIFY
    notifier = Notifier(wm, EventHandler())
    wm.add_watch(ftpMonitorLocalSyncDir, pyinotify.ALL_EVENTS, rec=True)
    sleepSeconds = int(execSeconds)
    if sleepSeconds > 30:
        sleepSeconds = 30
        print('对于监控目录而言，您配置的执行时间 execSeconds %s 过大, 已经自动为您调整成30秒 ' % execSeconds)
    while True:
        try:
            notifier.process_events()
            if notifier.check_events():
                notifier.read_events()
            time.sleep(sleepSeconds)
        except KeyboardInterrupt:
            notifier.stop()
            break

# 检测目录合法
class CheckVadidDirectory:
    def __init__(self):
        print('$$$$$$$$$$$$ 开始检测配置的目录是否正确 $$$$$$$$$$$$')
        self.checkVadidDir()
        self.checkVadiaFtpDir()
        print('$$$$$$$$$$$$ 恭喜你，配置的目录全部正确 $$$$$$$$$$$$')

    # check valid dir
    def checkVadidDir(self):
        if execType == 'upload':
            self.doCheck(ftpMonitorLocalSyncDir, True, '上传文件配置的本地目录检测 >>>>  非法!!!! 配置的目录末尾必须为斜线符号')
            self.doCheck(ftpUploadRemoteDir, False, 'ftp上传的远程目录检测 >>>>  非法!!!! 配置的目录末尾必须为斜线符号')
        elif execType == 'download':
            self.doCheck(ftpDownloadLocalDir, True, 'ftp下载之后存放的本地目录 >>>>  非法!!!!  配置的目录末尾必须为斜线符号')
            self.doCheck(ftpDownloadRemoteDir, False, 'ftp下载的远程目录 >>>>  非法!!!! 配置的目录末尾必须为斜线符号')

    def checkVadiaFtpDir(self):
        ftpConnect = FTPConnect(ftpHostIp, ftpAccount, ftpPassword)
        ftpConnect.checkFtpDir()
        ftpConnect.ftp.quit()


    def doCheck(self, checkDir, isLocal, invalidMsg):
        if checkDir.endswith('/') or checkDir.endswith('//'):
            if isLocal == True:
               flag = os.access(checkDir, os.F_OK)
               if flag == False:
                  raise RuntimeError("该目录不存在，请先创建!!!!")
        else:
            raise RuntimeError(invalidMsg)


# 检测目录input
class CheckExistDir:
    def __init__(self):
        self.checkDataExists()

    def printHeaderTip(self):
        print('\t-e <execType>               \t 执行类型，可选参数为 upload:上传 ， download:下载')
        print('\t-t <execSeconds>            \t 执行频率，多少秒执行一次，单位为秒')

    def printCommonTip(self):
        print('\t-i <ftpHostIp>              \t FTP服务器的IP')
        print('\t-u <ftpAccount>             \t FTP用户名')
        print('\t-p <ftpPassword>            \t FTP密码')

    def printUpLoadTip(self):
        self.printHeaderTip()
        self.printCommonTip()
        print('\t-a <actionType>             \t 触发类型，可选参数为 batchRead:批量读取目录列表文件 ，incrementRead:增量读取目录新增文件  注：批量读取文件列表会删除源文件进行上传，增量读取文件只会读取目录新增文件。默认采用batchRead。')
        print('\t-l <ftpMonitorLocalSyncDir> \t 监控文件的本地目录，上传到FTP服务器')
        print('\t-r <ftpDownloadRemoteDir>   \t 上传文件到FTP的远程目录')


    def printDownloadTip(self):
        self.printHeaderTip()
        self.printCommonTip()
        print('\t-l <ftpMonitorLocalSyncDir> \t 下载远程文件到本地目录')
        print('\t-r <ftpDownloadRemoteDir>   \t 下载FTP的远程目录')


    def printHeaderUsageMsg(self):
        print('usage:  python %s  -e <execType> -t <execSeconds>'  % sys.argv[0])


    def printUploadUsageMsg(self):
        print( 'usage:  python  %s -e upload  -t <execSeconds> -i <ftpHostIp> -u <ftpAccount> -p <ftpPassword> -a <actionType> -l <ftpMonitorLocalSyncDir> -r <ftpUploadRemoteDir>' % sys.argv[0])


    def printDownloadUsageMsg(self):
        print('usage:  python %s  -e upload -t <execSeconds> -i <ftpHostIp> -u <ftpAccount> -p <ftpPassword> -l <ftpDownloadLocalDir> -r <ftpDownloadRemoteDir>' % sys.argv[0])


    def printErrorMsg(self):
        print('您的参数书写错误,正确的方式如下：')


    def checkDataExists(self):
        try:
            opts, args = getopt.getopt(sys.argv[1:], 'he:t:i:u:p:a:l:r:x:y',
                                       ['help', 'execType=', 'execSeconds=', 'ftpHostIp=', 'ftpAccount=', 'ftpPassword=',
                                        'actionType=', 'ftpMonitorLocalSyncDir=', 'ftpUploadRemoteDir=', 'ftpDownloadLocalDir=',
                                        'ftpDownloadRemoteDir='])
        except getopt.GetoptError:
            self.printErrorMsg()
            self.printHeaderUsageMsg()
            sys.exit(2)
        if len(opts) == 0:
            self.printHeaderUsageMsg()
            print('缺少参数，您应该指定当前需要上传还是下载')
            sys.exit(2)
        for opt, arg in opts:
            if opt in ('-h', '--help'):
                self.printHeaderUsageMsg()
                self.printHeaderTip()
                sys.exit()
            elif opt in ('-e', '--execType'):
                if arg == 'upload':
                    try:
                        opts2, args2 = getopt.getopt(sys.argv[1:], 'he:t:i:u:p:a:l:r:',
                                                     ['help', 'actionType=', 'execType=', 'execSeconds=', 'ftpHostIp=', 'ftpAccount=',
                                                      'ftpPassword=', 'actionType=', 'ftpMonitorLocalSyncDir=', 'ftpUploadRemoteDir='])
                    except getopt.GetoptError:
                        self.printErrorMsg()
                        self.printUploadUsageMsg()
                    for opt2, arg2 in opts2:
                        if opt2 in ('-h', '--help'):
                            self.printUploadUsageMsg()
                            self.printUpLoadTip()
                            sys.exit()
                        elif opt2 in ('-a', '--actionType'):
                            global actionType
                            actionType = arg2
                        elif opt2 in ('-e', '--execType'):
                            global execType
                            execType = arg2
                        elif opt2 in ('-t', '--execSeconds'):
                            global execSeconds
                            execSeconds = arg2
                        elif opt2 in ('-i', '--ftpHostIp'):
                            global ftpHostIp
                            ftpHostIp = arg2
                        elif opt2 in ('-u', '--ftpAccount'):
                            global ftpAccount
                            ftpAccount = arg2
                        elif opt2 in ('-p', '--ftpPassword'):
                            global ftpPassword
                            ftpPassword = arg2
                        elif opt2 in ('-l', '--ftpMonitorLocalSyncDir'):
                            global ftpMonitorLocalSyncDir
                            ftpMonitorLocalSyncDir = arg2
                        elif opt2 in ('-r', '--ftpUploadRemoteDir'):
                            global ftpUploadRemoteDir
                            ftpUploadRemoteDir = arg2
                    if len(opts2) < 7:
                        self.printUploadUsageMsg()
                        sys.exit(2)
                elif arg == 'download':
                    try:
                        opts2, args2 = getopt.getopt(sys.argv[1:], 'he:t:i:u:p:l:r:',
                                                     ['help', 'execType=', 'execSeconds=', 'ftpHostIp=', 'ftpAccount=',
                                                      'ftpPassword', 'ftpDownloadLocalDir=', 'ftpDownloadRemoteDir='])
                    except getopt.GetoptError as e:
                        self.printErrorMsg()
                        self.printDownloadUsageMsg()
                    for opt2, arg2 in opts2:
                        if opt2 in ('-h', '--help'):
                            self.printDownloadUsageMsg()
                            self.printDownloadTip()
                            sys.exit()
                        elif opt2 in ('-e', '--execType'):
                            execType = arg2
                        elif opt2 in ('-t', '--execSeconds'):
                            execSeconds = arg2
                        elif opt2 in ('-i', '--ftpHostIp'):
                            ftpHostIp = arg2
                        elif opt2 in ('-u', '--ftpAccount'):
                            ftpAccount = arg2
                        elif opt2 in ('-p', '--ftpPassword'):
                            ftpPassword = arg2
                        elif opt2 in ('-l', '--ftpDownloadLocalDir'):
                            global ftpDownloadLocalDir
                            ftpDownloadLocalDir = arg2
                        elif opt2 in ('-r', '--ftpDownloadRemoteDir'):
                            global ftpDownloadRemoteDir
                            ftpDownloadRemoteDir = arg2

                    if len(opts2) < 7:
                        self.printDownloadUsageMsg()
                        sys.exit(2)
                else:
                    print('您输入的 -e <execType> 参数有问题，请检查是否在可选项 [upload, download]')
                    sys.exit(0)



if __name__ == '__main__':
    CheckExistDir()
    CheckVadidDirectory()

    if actionType == 'batchRead':
       runSchedule(int(execSeconds))
    elif actionType == 'incrementRead':
       monitorDir()
    else:
       print("您输入的 -a <actionType> 参数有问题，请检查是否在可选项 [batchRead, incrementRead]")
       sys.exit(0)