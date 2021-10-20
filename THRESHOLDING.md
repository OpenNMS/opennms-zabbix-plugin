# Thresholding

Zabbix 4 triggers: `{<server>:<key>.<function>(<parameter>)}<operator><constant>`
Zabbix 5 triggers: `function(/host/key,parameter)<operator><constant>`

## Zabbix 4
Example 1:
```
({Windows Server Template:perf_counter[\Processor(_Total)\% Processor Time,60].avg({$CPU_HIGH_DURATION})}>{$CPU_HIGH} and {TRIGGER.VALUE}=0) or ({Windows Server Template:perf_counter[\Processor(_Total)\% Processor Time,60].avg({$CPU_HIGH_DURATION})}>0.8*{$CPU_HIGH} and {TRIGGER.VALUE}=1)
```

Example 2:
```
{Windows Server Template:vm.memory.size[pavailable].max({$MEM_LOW_DURATION})}<{$MEM_LOW_PERC} and {Windows Server Template:vm.memory.size[free].last()}>=0
```

Example 3:
```
{Windows Server Template:vfs.fs.size[{#FSNAME},pfree].max(#4)}<{$DISK_PERCENT_LOW} and {Windows Server Template:vfs.fs.size[{#FSNAME},free].max(#4)}<{$DISK_LOW}
```

Example 4:
```
{Windows Server Template:vfs.fs.size[{#FSNAME},pfree].timeleft({$DISK_PFREE_PREDICTION_HISTORY},,{$DISK_PFREE_PREDICTION_TARGET})} < {$DISK_PFREE_PREDICTION_DURATION} and {Windows Server Template:vfs.fs.size[{#FSNAME},pfree].count(1h,,,{$DISK_PFREE_PREDICTION_HISTORY})}>0 and {Windows Server Template:vfs.fs.size[{#FSNAME},pfree].last()}>={$DISK_PFREE_PREDICTION_TARGET} and {Windows Server Template:vfs.fs.size[{#FSNAME},free].last()}>=0
```
