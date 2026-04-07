update t_coupon
set traffic_policy = 'HOT_FCFS_ASYNC'
where traffic_policy = 'NORMAL_SYNC';

alter table t_coupon
    alter column traffic_policy set default 'HOT_FCFS_ASYNC';
