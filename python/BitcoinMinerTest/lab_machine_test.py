import paramiko

def connect(hostname, clients):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(hostname, username='youname', password='yourpass', timeout=5.0)
    clients.append(ssh)

clients = []
# connect to Lin114 machines
for i in range(0,12):
    if i == 2:
        continue
    try:
        hostname = "lin114-" + "{0:02d}".format(i) + ".cise.ufl.edu"
        connect(hostname, clients)
        print "lin114", i
    except:
        pass
    
# connect to Lin115, lin116 machines
for i in range(1,24):
    try:
        hostname = "lin115-" + "{0:02d}".format(i) + ".cise.ufl.edu"
        connect(hostname, clients)
        print "lin115", i
    except:
        pass
    try:
        hostname = "lin116-" + "{0:02d}".format(i) + ".cise.ufl.edu"
        connect(hostname, clients)
        print "lin116", i
    except:
        pass
    
i = 1
for c in clients:
    try:
        c.exec_command('java -jar  Desktop/project1.jar lin114-02.cise.ufl.edu')
        # c.exec_command('ps -aux | grep java | awk \'{print $2}\' | xargs kill')
        print i
    except:
        pass
    i = i + 1


# for c in clients:
#     c.close()
