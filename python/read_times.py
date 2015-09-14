from pylab import *
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab

log_df = pd.read_csv("times_3.csv", names=['real_time', 'kernel_time', 'user_time'], na_values=['-'])
log_df['ratio'] = log_df.apply(lambda row: (row['kernel_time'] +row['user_time'])/row['real_time'], axis=1)
#pd.Series.plot(log_df['ratio'],kind='bar')
#show()
print log_df

xticks = np.arange(10000,1000000,100000)
print len(xticks)
yticks = log_df['ratio']
#plt.psd(xn, NFFT=150, Fs=fs, window=mlab.window_none, noverlap=75, pad_to=512,
 #   scale_by_freq=True)
plt.title('cores-utilisation')
#plt.xticks(xticks)
#plt.yticks(yticks)
plt.ylabel('ratio')
plt.xlabel('num-units')
#plt.grid(True)
plt.plot(xticks, yticks)
plt.show()
