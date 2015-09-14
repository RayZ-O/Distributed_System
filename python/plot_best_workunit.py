from subprocess import call
import subprocess
lead_zeroes = 5
num_units = 10000
total_units = 10000000
times = []
while (num_units <= 1000000):
	output = subprocess.check_output(["time","--format=%e,%S,%U", "--output=times.txt","-a","java","-jar","/home/sudeepgaddam/DOS/distributed_system/Scala/SimpleBitCoinMiner/target/scala-2.11/project1.jar",`num_units`, `total_units`, `lead_zeroes`])
	num_units+=10000
