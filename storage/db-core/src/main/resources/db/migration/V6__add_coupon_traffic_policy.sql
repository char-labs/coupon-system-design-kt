alter table t_coupon
    add column traffic_policy varchar(20) not null default 'NORMAL_SYNC';

create index idx_coupon_traffic_policy on t_coupon (traffic_policy);
