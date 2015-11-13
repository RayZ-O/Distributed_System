#!/usr/bin/env python2
# -*- coding: utf-8 -*-
from matplotlib.pyplot import *
import pandas as pd

log_df = pd.read_csv('/home/rui/Desktop/hops.txt',
names=['number','average hops'],
sep=' ')

log_df.plot(x='number', y='average hops')
xscale('log')
xlabel('number of peers')
ylabel('average number of hops')
show()
