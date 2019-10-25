#!/usr/bin/env python
# encoding:utf-8
# author guyuefeng 
import os
import sys
import getopt
from  pyinotify import  WatchManager, Notifier, \
ProcessEvent,IN_DELETE, IN_CREATE,IN_MODIFY
import pyinotify
import time
import subprocess
from subprocess import call

monitorDir=''

 
class EventHandler(ProcessEvent):
    flag = 0
    def process_IN_CREATE(self, event):
        print   "Create file: %s "  %   os.path.join(event.path,event.name)
 
    def process_IN_DELETE(self, event):
        print   "Delete file: %s "  %   os.path.join(event.path,event.name)
 
    def process_IN_MODIFY(self, event):
        print   "modify file: %s "  %   os.path.join(event.path,event.name)     
 
def FSMonitor(path='.'):
        wm = WatchManager() 
        mask = IN_DELETE | IN_CREATE |IN_MODIFY
        notifier = Notifier(wm, EventHandler())

	    pathDir = path[0:path.rfind('/')]
        wm.add_watch(pathDir , pyinotify.ALL_EVENTS ,rec=True)
        print 'now starting monitor %s' % (path)
        while True:
                try:
                        notifier.process_events()
                        if notifier.check_events():
                                notifier.read_events()
                        time.sleep(10)
                except KeyboardInterrupt:
                        notifier.stop()
                        break
 
if __name__ == "__main__":
    try:
    	opts,args = getopt.getopt(sys.argv[1:], 'he:g:', ['help','exeFileDir=','monitorFile='])
    except getopt.GetoptError:
    	print 'usage:  python ', sys.argv[0] , ' -e <exeFileDir> -g <monitorFile>'
        sys.exit(2)
    for opt, arg in opts:
        if opt in ('-h','--help'):
           print 'usage:  python ', sys.argv[0] , ' -e <exeFileDir> -g <monitorFile>'
           sys.exit()
        elif opt in ('-e','--exeFileDir'):
           exeFileDir = arg
        elif opt in ('-g','--monitorFile'):
           monitorFile = arg

    print exeFileDir +' ----- '+ monitorFile 
    if exeFileDir != '' and monitorFile != '': 
	    FSMonitor(monitorFile)
    else:
         print 'usage:	python ', sys.argv[0] , ' -e <exeFileDir> -g <monitorFile>'
         sys.exit(2)