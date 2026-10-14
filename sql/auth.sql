create table api_level_config
(
    id          int unsigned auto_increment
        primary key,
    api_key     varchar(50)                        not null comment '接口唯一编码（如 recipe:delete）',
    level_code  varchar(20)                        not null comment '需要的等级Key',
    description varchar(100)                       null comment '接口描述',
    updated_at  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_code
        unique (api_key)
)
    comment '接口等级配置表';

create table levels
(
    id          int unsigned auto_increment
        primary key,
    code        int                                not null comment '等级码（比较大小，如 1,2,3）',
    name        varchar(20)                        not null comment '等级名称（青铜/白银/黄金）',
    rule_min    int      default 0                 null comment '升级最小限制（计算规则）',
    icon_url    varchar(255)                       null comment '等级图标',
    description varchar(100)                       null comment '等级描述',
    created_at  datetime default CURRENT_TIMESTAMP null,
    constraint uk_code
        unique (code)
)
    comment '用户等级定义表';

create table users
(
    id            bigint unsigned auto_increment comment '主键'
        primary key,
    account       varchar(50)                                not null comment '登录账号',
    phone         varchar(30)                                not null comment '手机号',
    email         varchar(100)                               null comment '邮箱',
    nickname      varchar(30)                                not null comment '昵称',
    password      varchar(255)                               not null comment '密码哈希（bcrypt）',
    gender        tinyint unsigned default '0'               null comment '性别 0未知 1男 2女',
    avatar_url    varchar(500)                               null comment '头像链接',
    bio           varchar(200)                               null comment '个人简介',
    level         tinyint unsigned default '1'               null comment '用户等级（激励体系）',
    status        tinyint unsigned default '1'               null comment '状态 1正常 0封禁',
    last_login_at datetime                                   null comment '最后登录时间',
    created_at    datetime         default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at    datetime         default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted_at    bigint           default 0                 not null comment '软删除（0存在,时间戳则已删除）',
    constraint uk_account
        unique (account, deleted_at),
    constraint uk_phone
        unique (phone, deleted_at)
)
    comment '用户表';


