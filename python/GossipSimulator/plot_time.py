#!/usr/bin/env python2
# -*- coding: utf-8 -*-
from pylab import *
import pandas as pd

log_df = pd.read_csv('/home/rui/Desktop/time.txt',
names=['Number','Network','Time'],
sep=' ')

network = ['full', 'line', '2D', 'imp2D', '3D', 'imp3D']
for net in network:
	df = log_df[log_df['Network'] == net]
	df.plot(x='Number', y='Time')

legend(network, loc='upper right')
xlabel('number of peers')
ylabel('time(ms)')
xlim((0, 10000))
ylim((0.0, 20000.0))
show()
