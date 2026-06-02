-- ----------------------------
-- 1、部门表
-- ----------------------------
drop table if exists sys_dept;
create table sys_dept (
  dept_id           bigint(20)      not null auto_increment    comment '部门id',
  parent_id         bigint(20)      default 0                  comment '父部门id',
  ancestors         varchar(50)     default ''                 comment '祖级列表',
  dept_name         varchar(30)     default ''                 comment '部门名称',
  order_num         int(4)          default 0                  comment '显示顺序',
  leader            varchar(20)     default null               comment '负责人',
  phone             varchar(11)     default null               comment '联系电话',
  email             varchar(50)     default null               comment '邮箱',
  status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (dept_id)
) engine=innodb auto_increment=200 comment = '部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100,  0,   '0',          '若依科技',   0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(101,  100, '0,100',      '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(102,  100, '0,100',      '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(103,  101, '0,100,101',  '研发部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(104,  101, '0,100,101',  '市场部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(105,  101, '0,100,101',  '测试部门',   3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(106,  101, '0,100,101',  '财务部门',   4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(107,  101, '0,100,101',  '运维部门',   5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(108,  102, '0,100,102',  '市场部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(109,  102, '0,100,102',  '财务部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
  user_id           bigint(20)      not null auto_increment    comment '用户ID',
  dept_id           bigint(20)      default null               comment '部门ID',
  user_name         varchar(30)     not null                   comment '用户账号',
  nick_name         varchar(30)     not null                   comment '用户昵称',
  user_type         varchar(2)      default '00'               comment '用户类型（00系统用户）',
  email             varchar(50)     default ''                 comment '用户邮箱',
  phonenumber       varchar(11)     default ''                 comment '手机号码',
  sex               char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar            varchar(100)    default ''                 comment '头像地址',
  password          varchar(100)    default ''                 comment '密码',
  status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  pwd_update_date   datetime                                   comment '密码最后更新时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (user_id)
) engine=innodb auto_increment=100 comment = '用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user values(1,  103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '管理员');
insert into sys_user values(2,  105, 'ry',    '若依', '00', 'ry@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '测试员');


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
drop table if exists sys_post;
create table sys_post
(
  post_id       bigint(20)      not null auto_increment    comment '岗位ID',
  post_code     varchar(64)     not null                   comment '岗位编码',
  post_name     varchar(50)     not null                   comment '岗位名称',
  post_sort     int(4)          not null                   comment '显示顺序',
  status        char(1)         not null                   comment '状态（0正常 1停用）',
  create_by     varchar(64)     default ''                 comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     varchar(64)     default ''			       comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (post_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, 'ceo',  '董事长',    1, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(2, 'se',   '项目经理',  2, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(3, 'hr',   '人力资源',  3, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(4, 'user', '普通员工',  4, '0', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
  role_id              bigint(20)      not null auto_increment    comment '角色ID',
  role_name            varchar(30)     not null                   comment '角色名称',
  role_key             varchar(100)    not null                   comment '角色权限字符串',
  role_sort            int(4)          not null                   comment '显示顺序',
  data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
  status               char(1)         not null                   comment '角色状态（0正常 1停用）',
  del_flag             char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by            varchar(64)     default ''                 comment '创建者',
  create_time          datetime                                   comment '创建时间',
  update_by            varchar(64)     default ''                 comment '更新者',
  update_time          datetime                                   comment '更新时间',
  remark               varchar(500)    default null               comment '备注',
  primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '超级管理员');
insert into sys_role values('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', sysdate(), '', null, '普通角色');


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
  menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
  menu_name         varchar(50)     not null                   comment '菜单名称',
  parent_id         bigint(20)      default 0                  comment '父菜单ID',
  order_num         int(4)          default 0                  comment '显示顺序',
  path              varchar(200)    default ''                 comment '路由地址',
  component         varchar(255)    default null               comment '组件路径',
  query             varchar(255)    default null               comment '路由参数',
  route_name        varchar(50)     default ''                 comment '路由名称',
  is_frame          int(1)          default 1                  comment '是否为外链（0是 1否）',
  is_cache          int(1)          default 0                  comment '是否缓存（0缓存 1不缓存）',
  menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         default 0                  comment '菜单状态（0显示 1隐藏）',
  status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
  perms             varchar(100)    default null               comment '权限标识',
  icon              varchar(100)    default '#'                comment '菜单图标',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default ''                 comment '备注',
  primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system',           null, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', sysdate(), '', null, '系统管理目录');
insert into sys_menu values('2', '系统监控', '0', '2', 'monitor',          null, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', sysdate(), '', null, '系统监控目录');
insert into sys_menu values('3', '系统工具', '0', '3', 'tool',             null, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', sysdate(), '', null, '系统工具目录');
insert into sys_menu values('4', '若依官网', '0', '4', 'http://ruoyi.vip', null, '', '', 0, 0, 'M', '0', '0', '', 'guide',    'admin', sysdate(), '', null, '若依官网地址');
-- 二级菜单
insert into sys_menu values('100',  '用户管理', '1',   '1', 'user',       'system/user/index',        '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', sysdate(), '', null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理', '1',   '2', 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', sysdate(), '', null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理', '1',   '3', 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', sysdate(), '', null, '菜单管理菜单');
insert into sys_menu values('103',  '部门管理', '1',   '4', 'dept',       'system/dept/index',        '', '', 1, 0, 'C', '0', '0', 'system:dept:list',        'tree',          'admin', sysdate(), '', null, '部门管理菜单');
insert into sys_menu values('104',  '岗位管理', '1',   '5', 'post',       'system/post/index',        '', '', 1, 0, 'C', '0', '0', 'system:post:list',        'post',          'admin', sysdate(), '', null, '岗位管理菜单');
insert into sys_menu values('105',  '字典管理', '1',   '6', 'dict',       'system/dict/index',        '', '', 1, 0, 'C', '0', '0', 'system:dict:list',        'dict',          'admin', sysdate(), '', null, '字典管理菜单');
insert into sys_menu values('106',  '参数设置', '1',   '7', 'config',     'system/config/index',      '', '', 1, 0, 'C', '0', '0', 'system:config:list',      'edit',          'admin', sysdate(), '', null, '参数设置菜单');
insert into sys_menu values('107',  '通知公告', '1',   '8', 'notice',     'system/notice/index',      '', '', 1, 0, 'C', '0', '0', 'system:notice:list',      'message',       'admin', sysdate(), '', null, '通知公告菜单');
insert into sys_menu values('108',  '日志管理', '1',   '9', 'log',        '',                         '', '', 1, 0, 'M', '0', '0', '',                        'log',           'admin', sysdate(), '', null, '日志管理菜单');
insert into sys_menu values('109',  '在线用户', '2',   '1', 'online',     'monitor/online/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',     'online',        'admin', sysdate(), '', null, '在线用户菜单');
insert into sys_menu values('110',  '定时任务', '2',   '2', 'job',        'monitor/job/index',        '', '', 1, 0, 'C', '0', '0', 'monitor:job:list',        'job',           'admin', sysdate(), '', null, '定时任务菜单');
insert into sys_menu values('111',  '数据监控', '2',   '3', 'druid',      'monitor/druid/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',      'druid',         'admin', sysdate(), '', null, '数据监控菜单');
insert into sys_menu values('112',  '服务监控', '2',   '4', 'server',     'monitor/server/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',     'server',        'admin', sysdate(), '', null, '服务监控菜单');
insert into sys_menu values('113',  '缓存监控', '2',   '5', 'cache',      'monitor/cache/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis',         'admin', sysdate(), '', null, '缓存监控菜单');
insert into sys_menu values('114',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', sysdate(), '', null, '缓存列表菜单');
insert into sys_menu values('115',  '表单构建', '3',   '1', 'build',      'tool/build/index',         '', '', 1, 0, 'C', '0', '0', 'tool:build:list',         'build',         'admin', sysdate(), '', null, '表单构建菜单');
insert into sys_menu values('116',  '代码生成', '3',   '2', 'gen',        'tool/gen/index',           '', '', 1, 0, 'C', '0', '0', 'tool:gen:list',           'code',          'admin', sysdate(), '', null, '代码生成菜单');
insert into sys_menu values('117',  '系统接口', '3',   '3', 'swagger',    'tool/swagger/index',       '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',       'swagger',       'admin', sysdate(), '', null, '系统接口菜单');
-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          'admin', sysdate(), '', null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    'admin', sysdate(), '', null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1000', '用户查询', '100', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1001', '用户新增', '100', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1002', '用户修改', '100', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1003', '用户删除', '100', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1004', '用户导出', '100', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1005', '用户导入', '100', '6',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1006', '重置密码', '100', '7',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 'admin', sysdate(), '', null, '');
-- 角色管理按钮
insert into sys_menu values('1007', '角色查询', '101', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1008', '角色新增', '101', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1009', '角色修改', '101', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1010', '角色删除', '101', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1011', '角色导出', '101', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 'admin', sysdate(), '', null, '');
-- 菜单管理按钮
insert into sys_menu values('1012', '菜单查询', '102', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1013', '菜单新增', '102', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1014', '菜单修改', '102', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1015', '菜单删除', '102', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 'admin', sysdate(), '', null, '');
-- 部门管理按钮
insert into sys_menu values('1016', '部门查询', '103', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1017', '部门新增', '103', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1018', '部门修改', '103', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1019', '部门删除', '103', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove',         '#', 'admin', sysdate(), '', null, '');
-- 岗位管理按钮
insert into sys_menu values('1020', '岗位查询', '104', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1021', '岗位新增', '104', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1022', '岗位修改', '104', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1023', '岗位删除', '104', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1024', '岗位导出', '104', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export',         '#', 'admin', sysdate(), '', null, '');
-- 字典管理按钮
insert into sys_menu values('1025', '字典查询', '105', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1026', '字典新增', '105', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1027', '字典修改', '105', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1028', '字典删除', '105', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1029', '字典导出', '105', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export',         '#', 'admin', sysdate(), '', null, '');
-- 参数设置按钮
insert into sys_menu values('1030', '参数查询', '106', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1031', '参数新增', '106', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1032', '参数修改', '106', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1033', '参数删除', '106', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1034', '参数导出', '106', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export',       '#', 'admin', sysdate(), '', null, '');
-- 通知公告按钮
insert into sys_menu values('1035', '公告查询', '107', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1036', '公告新增', '107', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1037', '公告修改', '107', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1038', '公告删除', '107', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove',       '#', 'admin', sysdate(), '', null, '');
-- 操作日志按钮
insert into sys_menu values('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 'admin', sysdate(), '', null, '');
-- 登录日志按钮
insert into sys_menu values('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1045', '账户解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', sysdate(), '', null, '');
-- 在线用户按钮
insert into sys_menu values('1046', '在线查询', '109', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1047', '批量强退', '109', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1048', '单条强退', '109', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', sysdate(), '', null, '');
-- 定时任务按钮
insert into sys_menu values('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export',         '#', 'admin', sysdate(), '', null, '');
-- 代码生成按钮
insert into sys_menu values('1055', '生成查询', '116', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',             '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1056', '生成修改', '116', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',              '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1057', '生成删除', '116', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1058', '导入代码', '116', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1059', '预览代码', '116', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1060', '生成代码', '116', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',              '#', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
  user_id   bigint(20) not null comment '用户ID',
  role_id   bigint(20) not null comment '角色ID',
  primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('2', '2');


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
  role_id   bigint(20) not null comment '角色ID',
  menu_id   bigint(20) not null comment '菜单ID',
  primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('2', '1');
insert into sys_role_menu values ('2', '2');
insert into sys_role_menu values ('2', '3');
insert into sys_role_menu values ('2', '4');
insert into sys_role_menu values ('2', '100');
insert into sys_role_menu values ('2', '101');
insert into sys_role_menu values ('2', '102');
insert into sys_role_menu values ('2', '103');
insert into sys_role_menu values ('2', '104');
insert into sys_role_menu values ('2', '105');
insert into sys_role_menu values ('2', '106');
insert into sys_role_menu values ('2', '107');
insert into sys_role_menu values ('2', '108');
insert into sys_role_menu values ('2', '109');
insert into sys_role_menu values ('2', '110');
insert into sys_role_menu values ('2', '111');
insert into sys_role_menu values ('2', '112');
insert into sys_role_menu values ('2', '113');
insert into sys_role_menu values ('2', '114');
insert into sys_role_menu values ('2', '115');
insert into sys_role_menu values ('2', '116');
insert into sys_role_menu values ('2', '117');
insert into sys_role_menu values ('2', '500');
insert into sys_role_menu values ('2', '501');
insert into sys_role_menu values ('2', '1000');
insert into sys_role_menu values ('2', '1001');
insert into sys_role_menu values ('2', '1002');
insert into sys_role_menu values ('2', '1003');
insert into sys_role_menu values ('2', '1004');
insert into sys_role_menu values ('2', '1005');
insert into sys_role_menu values ('2', '1006');
insert into sys_role_menu values ('2', '1007');
insert into sys_role_menu values ('2', '1008');
insert into sys_role_menu values ('2', '1009');
insert into sys_role_menu values ('2', '1010');
insert into sys_role_menu values ('2', '1011');
insert into sys_role_menu values ('2', '1012');
insert into sys_role_menu values ('2', '1013');
insert into sys_role_menu values ('2', '1014');
insert into sys_role_menu values ('2', '1015');
insert into sys_role_menu values ('2', '1016');
insert into sys_role_menu values ('2', '1017');
insert into sys_role_menu values ('2', '1018');
insert into sys_role_menu values ('2', '1019');
insert into sys_role_menu values ('2', '1020');
insert into sys_role_menu values ('2', '1021');
insert into sys_role_menu values ('2', '1022');
insert into sys_role_menu values ('2', '1023');
insert into sys_role_menu values ('2', '1024');
insert into sys_role_menu values ('2', '1025');
insert into sys_role_menu values ('2', '1026');
insert into sys_role_menu values ('2', '1027');
insert into sys_role_menu values ('2', '1028');
insert into sys_role_menu values ('2', '1029');
insert into sys_role_menu values ('2', '1030');
insert into sys_role_menu values ('2', '1031');
insert into sys_role_menu values ('2', '1032');
insert into sys_role_menu values ('2', '1033');
insert into sys_role_menu values ('2', '1034');
insert into sys_role_menu values ('2', '1035');
insert into sys_role_menu values ('2', '1036');
insert into sys_role_menu values ('2', '1037');
insert into sys_role_menu values ('2', '1038');
insert into sys_role_menu values ('2', '1039');
insert into sys_role_menu values ('2', '1040');
insert into sys_role_menu values ('2', '1041');
insert into sys_role_menu values ('2', '1042');
insert into sys_role_menu values ('2', '1043');
insert into sys_role_menu values ('2', '1044');
insert into sys_role_menu values ('2', '1045');
insert into sys_role_menu values ('2', '1046');
insert into sys_role_menu values ('2', '1047');
insert into sys_role_menu values ('2', '1048');
insert into sys_role_menu values ('2', '1049');
insert into sys_role_menu values ('2', '1050');
insert into sys_role_menu values ('2', '1051');
insert into sys_role_menu values ('2', '1052');
insert into sys_role_menu values ('2', '1053');
insert into sys_role_menu values ('2', '1054');
insert into sys_role_menu values ('2', '1055');
insert into sys_role_menu values ('2', '1056');
insert into sys_role_menu values ('2', '1057');
insert into sys_role_menu values ('2', '1058');
insert into sys_role_menu values ('2', '1059');
insert into sys_role_menu values ('2', '1060');

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
drop table if exists sys_role_dept;
create table sys_role_dept (
  role_id   bigint(20) not null comment '角色ID',
  dept_id   bigint(20) not null comment '部门ID',
  primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
-- 初始化-角色和部门关联表数据
-- ----------------------------
insert into sys_role_dept values ('2', '100');
insert into sys_role_dept values ('2', '101');
insert into sys_role_dept values ('2', '105');


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
drop table if exists sys_user_post;
create table sys_user_post
(
  user_id   bigint(20) not null comment '用户ID',
  post_id   bigint(20) not null comment '岗位ID',
  primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values ('1', '1');
insert into sys_user_post values ('2', '2');


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
drop table if exists sys_oper_log;
create table sys_oper_log (
  oper_id           bigint(20)      not null auto_increment    comment '日志主键',
  title             varchar(50)     default ''                 comment '模块标题',
  business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(200)    default ''                 comment '方法名称',
  request_method    varchar(10)     default ''                 comment '请求方式',
  operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     default ''                 comment '操作人员',
  dept_name         varchar(50)     default ''                 comment '部门名称',
  oper_url          varchar(255)    default ''                 comment '请求URL',
  oper_ip           varchar(128)    default ''                 comment '主机地址',
  oper_location     varchar(255)    default ''                 comment '操作地点',
  oper_param        varchar(2000)   default ''                 comment '请求参数',
  json_result       varchar(2000)   default ''                 comment '返回参数',
  status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
  error_msg         varchar(2000)   default ''                 comment '错误消息',
  oper_time         datetime                                   comment '操作时间',
  cost_time         bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
) engine=innodb auto_increment=100 comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
drop table if exists sys_dict_type;
create table sys_dict_type
(
  dict_id          bigint(20)      not null auto_increment    comment '字典主键',
  dict_name        varchar(100)    default ''                 comment '字典名称',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_id),
  unique (dict_type)
) engine=innodb auto_increment=100 comment = '字典类型表';

insert into sys_dict_type values(1,  '用户性别', 'sys_user_sex',        '0', 'admin', sysdate(), '', null, '用户性别列表');
insert into sys_dict_type values(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', sysdate(), '', null, '菜单状态列表');
insert into sys_dict_type values(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', sysdate(), '', null, '系统开关列表');
insert into sys_dict_type values(4,  '任务状态', 'sys_job_status',      '0', 'admin', sysdate(), '', null, '任务状态列表');
insert into sys_dict_type values(5,  '任务分组', 'sys_job_group',       '0', 'admin', sysdate(), '', null, '任务分组列表');
insert into sys_dict_type values(6,  '系统是否', 'sys_yes_no',          '0', 'admin', sysdate(), '', null, '系统是否列表');
insert into sys_dict_type values(7,  '通知类型', 'sys_notice_type',     '0', 'admin', sysdate(), '', null, '通知类型列表');
insert into sys_dict_type values(8,  '通知状态', 'sys_notice_status',   '0', 'admin', sysdate(), '', null, '通知状态列表');
insert into sys_dict_type values(9,  '操作类型', 'sys_oper_type',       '0', 'admin', sysdate(), '', null, '操作类型列表');
insert into sys_dict_type values(10, '系统状态', 'sys_common_status',   '0', 'admin', sysdate(), '', null, '登录状态列表');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
drop table if exists sys_dict_data;
create table sys_dict_data
(
  dict_code        bigint(20)      not null auto_increment    comment '字典编码',
  dict_sort        int(4)          default 0                  comment '字典排序',
  dict_label       varchar(100)    default ''                 comment '字典标签',
  dict_value       varchar(100)    default ''                 comment '字典键值',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
  list_class       varchar(100)    default null               comment '表格回显样式',
  is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_code)
) engine=innodb auto_increment=100 comment = '字典数据表';

insert into sys_dict_data values(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', sysdate(), '', null, '性别男');
insert into sys_dict_data values(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别女');
insert into sys_dict_data values(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别未知');
insert into sys_dict_data values(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '显示菜单');
insert into sys_dict_data values(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '隐藏菜单');
insert into sys_dict_data values(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');
insert into sys_dict_data values(8,  1,  '正常',     '0',       'sys_job_status',      '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(9,  2,  '暂停',     '1',       'sys_job_status',      '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');
insert into sys_dict_data values(10, 1,  '默认',     'DEFAULT', 'sys_job_group',       '',   '',        'Y', '0', 'admin', sysdate(), '', null, '默认分组');
insert into sys_dict_data values(11, 2,  '系统',     'SYSTEM',  'sys_job_group',       '',   '',        'N', '0', 'admin', sysdate(), '', null, '系统分组');
insert into sys_dict_data values(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '系统默认是');
insert into sys_dict_data values(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '系统默认否');
insert into sys_dict_data values(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 'admin', sysdate(), '', null, '通知');
insert into sys_dict_data values(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', sysdate(), '', null, '公告');
insert into sys_dict_data values(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '关闭状态');
insert into sys_dict_data values(18, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '其他操作');
insert into sys_dict_data values(19, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '新增操作');
insert into sys_dict_data values(20, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '修改操作');
insert into sys_dict_data values(21, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '删除操作');
insert into sys_dict_data values(22, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '授权操作');
insert into sys_dict_data values(23, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导出操作');
insert into sys_dict_data values(24, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导入操作');
insert into sys_dict_data values(25, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '强退操作');
insert into sys_dict_data values(26, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '生成操作');
insert into sys_dict_data values(27, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '清空操作');
insert into sys_dict_data values(28, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(29, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
drop table if exists sys_config;
create table sys_config (
  config_id         int(5)          not null auto_increment    comment '参数主键',
  config_name       varchar(100)    default ''                 comment '参数名称',
  config_key        varchar(100)    default ''                 comment '参数键名',
  config_value      varchar(500)    default ''                 comment '参数键值',
  config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (config_id)
) engine=innodb auto_increment=100 comment = '参数配置表';

insert into sys_config values(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',               'skin-blue',     'Y', 'admin', sysdate(), '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '用户管理-账号初始密码',         'sys.user.initPassword',            '123456',        'Y', 'admin', sysdate(), '', null, '初始化密码 123456' );
insert into sys_config values(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',              'theme-dark',    'Y', 'admin', sysdate(), '', null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',       'true',          'Y', 'admin', sysdate(), '', null, '是否开启验证码功能（true开启，false关闭）');
insert into sys_config values(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',         'false',         'Y', 'admin', sysdate(), '', null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(6, '用户登录-黑名单列表',           'sys.login.blackIPList',            '',              'Y', 'admin', sysdate(), '', null, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
insert into sys_config values(7, '用户管理-初始密码修改策略',     'sys.account.initPasswordModify',   '1',             'Y', 'admin', sysdate(), '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
insert into sys_config values(8, '用户管理-账号密码更新周期',     'sys.account.passwordValidateDays', '0',             'Y', 'admin', sysdate(), '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
insert into sys_config values(9, '用户管理-密码字符范围',         'sys.account.chrtype',              '0',             'Y', 'admin', sysdate(), '', null, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
drop table if exists sys_logininfor;
create table sys_logininfor (
  info_id        bigint(20)     not null auto_increment   comment '访问ID',
  user_name      varchar(50)    default ''                comment '用户账号',
  ipaddr         varchar(128)   default ''                comment '登录IP地址',
  login_location varchar(255)   default ''                comment '登录地点',
  browser        varchar(50)    default ''                comment '浏览器类型',
  os             varchar(50)    default ''                comment '操作系统',
  status         char(1)        default '0'               comment '登录状态（0成功 1失败）',
  msg            varchar(255)   default ''                comment '提示消息',
  login_time     datetime                                 comment '访问时间',
  primary key (info_id),
  key idx_sys_logininfor_s  (status),
  key idx_sys_logininfor_lt (login_time)
) engine=innodb auto_increment=100 comment = '系统访问记录';


-- ----------------------------
-- 15、定时任务调度表
-- ----------------------------
drop table if exists sys_job;
create table sys_job (
  job_id              bigint(20)    not null auto_increment    comment '任务ID',
  job_name            varchar(64)   default ''                 comment '任务名称',
  job_group           varchar(64)   default 'DEFAULT'          comment '任务组名',
  invoke_target       varchar(500)  not null                   comment '调用目标字符串',
  cron_expression     varchar(255)  default ''                 comment 'cron执行表达式',
  misfire_policy      varchar(20)   default '3'                comment '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  concurrent          char(1)       default '1'                comment '是否并发执行（0允许 1禁止）',
  status              char(1)       default '0'                comment '状态（0正常 1暂停）',
  create_by           varchar(64)   default ''                 comment '创建者',
  create_time         datetime                                 comment '创建时间',
  update_by           varchar(64)   default ''                 comment '更新者',
  update_time         datetime                                 comment '更新时间',
  remark              varchar(500)  default ''                 comment '备注信息',
  primary key (job_id, job_name, job_group)
) engine=innodb auto_increment=100 comment = '定时任务调度表';

insert into sys_job values(1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams',        '0/10 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');
insert into sys_job values(2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')',  '0/15 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');
insert into sys_job values(3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)',  '0/20 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 16、定时任务调度日志表
-- ----------------------------
drop table if exists sys_job_log;
create table sys_job_log (
  job_log_id          bigint(20)     not null auto_increment    comment '任务日志ID',
  job_name            varchar(64)    not null                   comment '任务名称',
  job_group           varchar(64)    not null                   comment '任务组名',
  invoke_target       varchar(500)   not null                   comment '调用目标字符串',
  job_message         varchar(500)                              comment '日志信息',
  status              char(1)        default '0'                comment '执行状态（0正常 1失败）',
  exception_info      varchar(2000)  default ''                 comment '异常信息',
  start_time          datetime                                  comment '执行开始时间',
  end_time            datetime                                  comment '执行结束时间',
  create_time         datetime                                  comment '创建时间',
  primary key (job_log_id)
) engine=innodb comment = '定时任务调度日志表';


-- ----------------------------
-- 17、通知公告表
-- ----------------------------
drop table if exists sys_notice;
create table sys_notice (
  notice_id         int(4)          not null auto_increment    comment '公告ID',
  notice_title      varchar(50)     not null                   comment '公告标题',
  notice_type       char(1)         not null                   comment '公告类型（1通知 2公告）',
  notice_content    longblob        default null               comment '公告内容',
  status            char(1)         default '0'                comment '公告状态（0正常 1关闭）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(255)    default null               comment '备注',
  primary key (notice_id)
) engine=innodb auto_increment=10 comment = '通知公告表';

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------
insert into sys_notice values('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', sysdate(), '', null, '管理员');
insert into sys_notice values('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容',   '0', 'admin', sysdate(), '', null, '管理员');
insert into sys_notice values('3', '若依开源框架介绍', '1', '<p><span style=\"color: rgb(230, 0, 0);\">项目介绍</span></p><p><font color=\"#333333\">RuoYi开源项目是为企业用户定制的后台脚手架框架，为企业打造的一站式解决方案，降低企业开发成本，提升开发效率。主要包括用户管理、角色管理、部门管理、菜单管理、参数管理、字典管理、</font><span style=\"color: rgb(51, 51, 51);\">岗位管理</span><span style=\"color: rgb(51, 51, 51);\">、定时任务</span><span style=\"color: rgb(51, 51, 51);\">、</span><span style=\"color: rgb(51, 51, 51);\">服务监控、登录日志、操作日志、代码生成等功能。其中，还支持多数据源、数据权限、国际化、Redis缓存、Docker部署、滑动验证码、第三方认证登录、分布式事务、</span><font color=\"#333333\">分布式文件存储</font><span style=\"color: rgb(51, 51, 51);\">、分库分表处理等技术特点。</span></p><p><img src=\"https://foruda.gitee.com/images/1773931848342439032/a4d22313_1815095.png\" style=\"width: 64px;\"><br></p><p><span style=\"color: rgb(230, 0, 0);\">官网及演示</span></p><p><span style=\"color: rgb(51, 51, 51);\">若依官网地址：&nbsp;</span><a href=\"http://ruoyi.vip\" target=\"_blank\">http://ruoyi.vip</a><a href=\"http://ruoyi.vip\" target=\"_blank\"></a></p><p><span style=\"color: rgb(51, 51, 51);\">若依文档地址：&nbsp;</span><a href=\"http://doc.ruoyi.vip\" target=\"_blank\">http://doc.ruoyi.vip</a><br></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【不分离版】：&nbsp;</span><a href=\"http://demo.ruoyi.vip\" target=\"_blank\">http://demo.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【分离版本】：&nbsp;</span><a href=\"http://vue.ruoyi.vip\" target=\"_blank\">http://vue.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【微服务版】：&nbsp;</span><a href=\"http://cloud.ruoyi.vip\" target=\"_blank\">http://cloud.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【移动端版】：&nbsp;</span><a href=\"http://h5.ruoyi.vip\" target=\"_blank\">http://h5.ruoyi.vip</a></p><p><br style=\"color: rgb(48, 49, 51); font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 12px;\"></p>', '0', 'admin', sysdate(), '', null, '管理员');


-- ----------------------------
-- 18、公告已读记录表
-- ----------------------------
drop table if exists sys_notice_read;
create table sys_notice_read (
  read_id          bigint(20)       not null auto_increment    comment '已读主键',
  notice_id        int(4)           not null                   comment '公告id',
  user_id          bigint(20)       not null                   comment '用户id',
  read_time        datetime         not null                   comment '阅读时间',
  primary key (read_id),
  unique key uk_user_notice (user_id, notice_id)   comment '同一用户同一公告只记录一次'
) engine=innodb auto_increment=1 comment='公告已读记录表';


-- ----------------------------
-- 19、代码生成业务表
-- ----------------------------
drop table if exists gen_table;
create table gen_table (
  table_id          bigint(20)      not null auto_increment    comment '编号',
  table_name        varchar(200)    default ''                 comment '表名称',
  table_comment     varchar(500)    default ''                 comment '表描述',
  sub_table_name    varchar(64)     default null               comment '关联子表的表名',
  sub_table_fk_name varchar(64)     default null               comment '子表关联的外键名',
  class_name        varchar(100)    default ''                 comment '实体类名称',
  tpl_category      varchar(200)    default 'crud'             comment '使用的模板（crud单表操作 tree树表操作）',
  tpl_web_type      varchar(30)     default ''                 comment '前端模板类型（element-ui模版 element-plus模版）',
  package_name      varchar(100)                               comment '生成包路径',
  module_name       varchar(30)                                comment '生成模块名',
  business_name     varchar(30)                                comment '生成业务名',
  function_name     varchar(50)                                comment '生成功能名',
  function_author   varchar(50)                                comment '生成功能作者',
  form_col_num      int(1)          default 1                  comment '表单布局（单列 双列 三列）',
  gen_type          char(1)         default '0'                comment '生成代码方式（0zip压缩包 1自定义路径）',
  gen_path          varchar(200)    default '/'                comment '生成路径（不填默认项目路径）',
  options           varchar(1000)                              comment '其它生成选项',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (table_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表';


-- ----------------------------
-- 20、代码生成业务表字段
-- ----------------------------
drop table if exists gen_table_column;
create table gen_table_column (
  column_id         bigint(20)      not null auto_increment    comment '编号',
  table_id          bigint(20)                                 comment '归属表编号',
  column_name       varchar(200)                               comment '列名称',
  column_comment    varchar(500)                               comment '列描述',
  column_type       varchar(100)                               comment '列类型',
  java_type         varchar(500)                               comment 'JAVA类型',
  java_field        varchar(200)                               comment 'JAVA字段名',
  is_pk             char(1)                                    comment '是否主键（1是）',
  is_increment      char(1)                                    comment '是否自增（1是）',
  is_required       char(1)                                    comment '是否必填（1是）',
  is_insert         char(1)                                    comment '是否为插入字段（1是）',
  is_edit           char(1)                                    comment '是否编辑字段（1是）',
  is_list           char(1)                                    comment '是否列表字段（1是）',
  is_query          char(1)                                    comment '是否查询字段（1是）',
  query_type        varchar(200)    default 'EQ'               comment '查询方式（等于、不等于、大于、小于、范围）',
  html_type         varchar(200)                               comment '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  dict_type         varchar(200)    default ''                 comment '字典类型',
  sort              int                                        comment '排序',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (column_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表字段';


-- ----------------------------
-- 21、作业计划表
-- ----------------------------
drop table if exists hw_plan;
create table hw_plan (
  plan_id           bigint(20)     not null auto_increment  comment '计划ID',
  city_county       varchar(100)   default ''               comment '市/县',
  construction_site varchar(200)   default ''               comment '施工点',
  site_latitude     decimal(10,7)  default null             comment '施工点纬度',
  site_longitude    decimal(10,7)  default null             comment '施工点经度',
  plan_work_time    datetime       default null             comment '计划作业时间',
  project_name      varchar(200)   default ''               comment '项目名称',
  work_type         varchar(20)    default ''               comment '作业类型（动土/防腐/检测/临时用电/受限空间/机械作业/修复/点火）',
  construction_unit varchar(200)   default ''               comment '施工单位',
  workers           varchar(500)   default ''               comment '参与施工各类人员',
  work_content      varchar(1000)  default ''               comment '作业内容',
  status            char(1)        default '0'              comment '状态（0待执行 1进行中 2已完成 3已取消）',
  create_by         varchar(64)    default ''               comment '创建者',
  create_time       datetime                                comment '创建时间',
  update_by         varchar(64)    default ''               comment '更新者',
  update_time       datetime                                comment '更新时间',
  remark            varchar(500)   default null             comment '备注',
  primary key (plan_id)
) engine=innodb comment='作业计划表';

-- ----------------------------
-- 22、施工人员表
-- ----------------------------
drop table if exists hw_worker;
create table hw_worker (
  worker_id    bigint(20)     not null auto_increment  comment '人员ID',
  worker_name  varchar(64)    not null                  comment '姓名',
  id_card      varchar(18)    default ''               comment '身份证号',
  phone        varchar(20)    default ''               comment '手机号',
  role_type    char(1)        default '9'               comment '人员角色（1作业申请人 2作业批准人 3作业监护人 4监理人员 5施工方项目经理 6施工方安全员 7施工方现场负责人 8作业单位监护人 9施工人员）',
  unit_type    char(1)        default '3'               comment '单位类型（1管网 2第三方 3施工方）',
  is_fixed_site char(1)       default '0'               comment '固定工点（0不固定 1固定）',
  check_rule   varchar(100)   default ''               comment '打卡规则编码',
  qualification varchar(200)  default ''               comment '资质证件名称',
  qual_file_url varchar(500)  default ''               comment '资质证件上传URL',
  qual_status  char(1)        default '0'               comment '资质审核状态（0待审核 1已通过 2已驳回）',
  face_image   varchar(500)   default ''               comment '人脸底图URL',
  face_status  char(1)        default '0'               comment '人脸注册状态（0未注册 1已注册）',
  open_id      varchar(100)   default ''               comment '微信公众号openid',
  status       char(1)        default '0'               comment '状态（0正常 1停用）',
  create_by    varchar(64)    default ''               comment '创建者',
  create_time  datetime                                comment '创建时间',
  update_by    varchar(64)    default ''               comment '更新者',
  update_time  datetime                                comment '更新时间',
  remark       varchar(500)   default null             comment '备注',
  primary key (worker_id)
) engine=innodb comment='施工人员表';

-- ----------------------------
-- 23、打卡记录表
-- ----------------------------
drop table if exists hw_attendance;
create table hw_attendance (
  attendance_id bigint(20)     not null auto_increment  comment '打卡记录ID',
  plan_id       bigint(20)     not null                  comment '关联作业计划ID',
  user_id       bigint(20)     default null              comment '打卡人员用户ID',
  user_name     varchar(64)    default ''               comment '打卡人员姓名',
  check_type    char(1)        default '0'               comment '打卡类型（0进场 1离场 2点到 3每小时点到）',
  check_method  char(1)        default '0'               comment '打卡方式（0人脸 1公众号）',
  check_time    datetime       default null              comment '打卡时间',
  location      varchar(255)   default ''               comment '打卡位置',
  check_status  char(1)        default '0'               comment '打卡状态（0成功 1失败 2异常）',
  fail_reason   varchar(500)   default ''               comment '失败原因',
  face_image    varchar(500)   default ''               comment '人脸抓拍图URL',
  create_by     varchar(64)    default ''               comment '创建者',
  create_time   datetime                                comment '创建时间',
  update_by     varchar(64)    default ''               comment '更新者',
  update_time   datetime                                comment '更新时间',
  remark        varchar(500)   default null              comment '备注',
  primary key (attendance_id)
) engine=innodb comment='打卡记录表';

-- ----------------------------
-- 24、作业管理菜单数据
-- ----------------------------
-- 更新若依官网排序号
update sys_menu set order_num = 5 where menu_id = 4;

-- 一级目录"作业管理"
insert into sys_menu values
((select max(menu_id)+1 from sys_menu m), '作业管理', '0', '4', 'homework', null, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', null, '作业管理目录');

-- 二级菜单
set @parent_id = (select menu_id from sys_menu where menu_name = '作业管理' and parent_id = 0);

insert into sys_menu values
(@parent_id+1, '作业计划', @parent_id, '1', 'plan',       'homework/plan/index',       '', 'HwPlan',       1, 0, 'C', '0', '0', 'homework:plan:list',        'build',   'admin', sysdate(), '', null, '作业计划菜单'),
(@parent_id+2, '作业打卡', @parent_id, '2', 'attendance', 'homework/attendance/index',  '', 'HwAttendance', 1, 0, 'C', '0', '0', 'homework:attendance:list',  'monitor', 'admin', sysdate(), '', null, '作业打卡菜单'),
(@parent_id+3, '人员管理', @parent_id, '3', 'worker',     'homework/worker/index',      '', 'HwWorker',     1, 0, 'C', '0', '0', 'homework:worker:list',      'user',    'admin', sysdate(), '', null, '人员管理菜单');

-- 作业计划按钮权限（偏移 +11）
set @plan_id = (select menu_id from sys_menu where perms = 'homework:plan:list');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) values
(@plan_id+11, '作业查询', @plan_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:query',   '#', 'admin', sysdate(), '', null, ''),
(@plan_id+12, '作业新增', @plan_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:add',     '#', 'admin', sysdate(), '', null, ''),
(@plan_id+13, '作业修改', @plan_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:edit',    '#', 'admin', sysdate(), '', null, ''),
(@plan_id+14, '作业删除', @plan_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:remove',  '#', 'admin', sysdate(), '', null, '');

-- 作业打卡按钮权限（偏移 +101，拉开与 plan 的距离）
set @att_id = (select menu_id from sys_menu where perms = 'homework:attendance:list');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) values
(@att_id+101, '打卡查询',  @att_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:query',    '#', 'admin', sysdate(), '', null, ''),
(@att_id+102, '进场打卡',  @att_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:checkIn',  '#', 'admin', sysdate(), '', null, ''),
(@att_id+103, '离场打卡',  @att_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:checkOut', '#', 'admin', sysdate(), '', null, ''),
(@att_id+104, '删除记录',  @att_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:remove',   '#', 'admin', sysdate(), '', null, '');

-- 人员管理按钮权限（偏移 +201）
set @worker_id = (select menu_id from sys_menu where perms = 'homework:worker:list');
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) values
(@worker_id+201, '人员查询', @worker_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:query',  '#', 'admin', sysdate(), '', null, ''),
(@worker_id+202, '人员新增', @worker_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:add',    '#', 'admin', sysdate(), '', null, ''),
(@worker_id+203, '人员修改', @worker_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:edit',   '#', 'admin', sysdate(), '', null, ''),
(@worker_id+204, '人员删除', @worker_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:remove', '#', 'admin', sysdate(), '', null, '');

-- 定时任务：作业打卡超时监控
insert into sys_job values
((select max(job_id)+1 from sys_job j), '作业打卡超时监控', 'SYSTEM', 'hwAttendanceTimeoutJob.execute', '0 */1 * * * ?', '0', '0', '0', 'admin', sysdate(), '', null, '监控作业打卡进场和离场超时情况');

-- 示例数据（使用 auto_increment，不指定主键）
insert into hw_plan (city_county, construction_site, site_latitude, site_longitude, plan_work_time, project_name, work_type, construction_unit, workers, work_content, status, create_by, create_time) values
('广州市', '天河区体育西路工地', 23.1234567, 113.1234567, '2026-06-15 08:00:00', '管道维修项目A', '动土', '中建三局', '张三,李四,王五', 'DN300管道开挖修复', '0', 'admin', sysdate());
insert into hw_worker (worker_name, id_card, phone, role_type, unit_type, is_fixed_site, check_rule, status, create_by, create_time) values
('张三', '440101199001011234', '13800138001', '9', '3', '1', 'briefing', '0', 'admin', sysdate());
insert into hw_worker (worker_name, id_card, phone, role_type, unit_type, is_fixed_site, check_rule, status, create_by, create_time) values
('李四', '440101199002022345', '13800138002', '1', '1', '0', 'point', '0', 'admin', sysdate());

-- ----------------------------
-- 25、作业计划人员关联表
-- ----------------------------
DROP TABLE IF EXISTS hw_plan_worker;
CREATE TABLE hw_plan_worker (
  id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
  plan_id     BIGINT(20)   NOT NULL                  COMMENT '作业计划ID',
  worker_id   BIGINT(20)   NOT NULL                  COMMENT '人员ID',
  worker_name VARCHAR(64)  DEFAULT ''               COMMENT '人员姓名（冗余）',
  role_type   CHAR(1)      DEFAULT ''               COMMENT '人员角色（冗余）',
  create_by   VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time DATETIME                               COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_plan_id (plan_id),
  INDEX idx_worker_id (worker_id)
) ENGINE=InnoDB COMMENT='作业计划人员关联表';

-- ----------------------------
-- 26、作业计划录像关联表
-- ----------------------------
DROP TABLE IF EXISTS hw_plan_video;
CREATE TABLE hw_plan_video (
  id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
  plan_id     BIGINT(20)   NOT NULL                  COMMENT '作业计划ID',
  record_id   BIGINT(20)   NOT NULL                  COMMENT '录像记录ID',
  record_name VARCHAR(200) DEFAULT ''               COMMENT '录像文件名（冗余）',
  start_time  DATETIME     DEFAULT NULL              COMMENT '录像开始时间（冗余）',
  end_time    DATETIME     DEFAULT NULL              COMMENT '录像结束时间（冗余）',
  create_by   VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time DATETIME                               COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB COMMENT='作业计划录像关联表';


-- ============================================================
-- 27、人员信息与资质管理模块（tb_worker* 体系）
-- 来源：ruoyiV1 合并（Zisaac52），原 7 个 SQL 文件合并为一个 section
-- ============================================================
USE `ry-vue`;

-- ---------------------------------------------------------
-- 27.1 tb_worker —— 人员基础档案（核心）
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker`;
CREATE TABLE `tb_worker` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '人员ID',
  `worker_name`  varchar(50)  NOT NULL                COMMENT '姓名',
  `phone`        varchar(20)  DEFAULT ''              COMMENT '手机号',
  `id_card`      varchar(20)  DEFAULT ''              COMMENT '身份证号',
  `gender`       char(1)      DEFAULT '0'             COMMENT '性别（字典 sys_user_sex：0男 1女 2未知）',
  `dept_id`      bigint       DEFAULT NULL            COMMENT '所属单位（关联 sys_dept.dept_id）',
  `status`       char(1)      DEFAULT '0'             COMMENT '人员状态（字典 worker_status：0在场 1离场 2禁用）',
  `face_status`  char(1)      DEFAULT '0'             COMMENT '人脸录入状态（字典 worker_face_status：0未录入 1已录入）',
  `audit_status` char(1)      DEFAULT '0'             COMMENT '审核状态（字典 worker_audit_status：0待审核 1已通过 2已驳回 3已过期）',
  `unit_type`    char(1)      DEFAULT '3'             COMMENT '单位类型（字典 worker_unit_type：1管网 2第三方 3施工方）',
  `del_flag`     char(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员基础档案';

-- ---------------------------------------------------------
-- 27.2 tb_worker_role —— 人员角色规则
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_role`;
CREATE TABLE `tb_worker_role` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '角色规则ID',
  `role_code`         varchar(50)  NOT NULL                COMMENT '角色编码',
  `role_name`         varchar(50)  NOT NULL                COMMENT '角色名称',
  `unit_type`         char(1)      DEFAULT ''              COMMENT '单位类型',
  `fixed_site_flag`   char(1)      DEFAULT '0'             COMMENT '是否固定工点（0否 1是）',
  `need_sign_in`      char(1)      DEFAULT '0'             COMMENT '是否需要签到（0否 1是）',
  `need_sign_out`     char(1)      DEFAULT '0'             COMMENT '是否需要签退（0否 1是）',
  `need_hourly_check` char(1)      DEFAULT '0'             COMMENT '是否需要点到（0否 1是）',
  `hourly_interval`   int          DEFAULT NULL            COMMENT '点到间隔（分钟）',
  `need_cert`         char(1)      DEFAULT '0'             COMMENT '是否需要资质（0否 1是）',
  `cert_type`         varchar(50)  DEFAULT ''              COMMENT '所需资质类型',
  `status`            char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `create_by`         varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`       datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`         varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`       datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`            varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员角色规则';

-- ---------------------------------------------------------
-- 27.3 tb_worker_role_rel —— 人员角色多对多关联
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_role_rel`;
CREATE TABLE `tb_worker_role_rel` (
  `id`        bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worker_id` bigint NOT NULL                COMMENT '人员ID',
  `role_id`   bigint NOT NULL                COMMENT '角色规则ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_worker_role` (`worker_id`, `role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员角色关联';

-- ---------------------------------------------------------
-- 27.4 tb_worker_face —— 人脸信息
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_face`;
CREATE TABLE `tb_worker_face` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worker_id`    bigint       NOT NULL                COMMENT '人员ID',
  `face_img_url` varchar(500) DEFAULT ''              COMMENT '人脸照片URL',
  `face_feature` text                                 COMMENT '人脸特征值（AI生成）',
  `collect_time` datetime     DEFAULT NULL            COMMENT '采集时间',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸信息';

-- ---------------------------------------------------------
-- 27.5 tb_worker_cert —— 资质证件
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_cert`;
CREATE TABLE `tb_worker_cert` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worker_id`    bigint       NOT NULL                COMMENT '人员ID',
  `cert_type`    varchar(50)  DEFAULT ''              COMMENT '证件类型',
  `cert_no`      varchar(100) DEFAULT ''              COMMENT '证件编号',
  `issue_date`   date         DEFAULT NULL            COMMENT '发证日期',
  `expire_date`  date         DEFAULT NULL            COMMENT '过期日期',
  `cert_img`     varchar(500) DEFAULT ''              COMMENT '证件图片URL',
  `audit_status` char(1)      DEFAULT '0'             COMMENT '审核状态（0待审核 1已通过 2已驳回 3已过期）',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_worker` (`worker_id`),
  KEY `idx_expire` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资质证件';

-- ---------------------------------------------------------
-- 27.6 tb_worker_audit —— 资料审核记录
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_audit`;
CREATE TABLE `tb_worker_audit` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`      varchar(30)  DEFAULT ''              COMMENT '审核业务类型（worker人员 / cert资质）',
  `biz_id`        bigint       DEFAULT NULL            COMMENT '业务数据ID',
  `worker_id`     bigint       DEFAULT NULL            COMMENT '关联人员ID',
  `audit_status`  char(1)      DEFAULT '0'             COMMENT '审核结果',
  `audit_opinion` varchar(500) DEFAULT ''              COMMENT '审核意见 / 驳回原因',
  `auditor`       varchar(64)  DEFAULT ''              COMMENT '审核人',
  `audit_time`    datetime     DEFAULT NULL            COMMENT '审核时间',
  `create_by`     varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料审核记录';

-- ---------------------------------------------------------
-- 27.7 tb_worker_checkin —— 打卡记录（签到/签退/点到）
-- ---------------------------------------------------------
DROP TABLE IF EXISTS `tb_worker_checkin`;
CREATE TABLE `tb_worker_checkin` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `worker_id`    bigint       NOT NULL                COMMENT '人员ID',
  `role_id`      bigint       DEFAULT NULL            COMMENT '打卡时角色ID',
  `check_type`   char(1)      DEFAULT ''              COMMENT '打卡类型（1签到 2签退 3点到）',
  `check_time`   datetime     DEFAULT NULL            COMMENT '打卡时间',
  `check_method` varchar(20)  DEFAULT ''              COMMENT '打卡方式（AI / 公众号 / 手动）',
  `site_id`      bigint       DEFAULT NULL            COMMENT '工点ID',
  `latitude`     double       DEFAULT NULL            COMMENT 'GPS纬度',
  `longitude`    double       DEFAULT NULL            COMMENT 'GPS经度',
  `photo_url`    varchar(500) DEFAULT ''              COMMENT '现场照片URL',
  `ai_result`    varchar(500) DEFAULT ''              COMMENT 'AI识别结果（JSON）',
  `helmet_flag`  char(1)      DEFAULT NULL            COMMENT '安全帽（0未戴 1已戴）',
  `vest_flag`    char(1)      DEFAULT NULL            COMMENT '反光衣（0未穿 1已穿）',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_worker` (`worker_id`),
  KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录';

-- ---------------------------------------------------------
-- 27.8 顶级菜单「人员管理」+ 6 个子菜单 + 按钮权限
-- ---------------------------------------------------------
INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
  ('人员管理', 0, 0, 'worker', NULL, '', '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', NOW(), '人员信息与资质管理（实名制底座）');
SET @worker_dir_id := LAST_INSERT_ID();

-- 子菜单1：人员档案
INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
  ('人员档案', @worker_dir_id, 1, 'worker', 'worker/worker/index', '', '', 1, 0, 'C', '0', '0', 'worker:worker:list', 'user', 'admin', NOW(), '人员基础档案管理');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('人员档案查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:worker:query',  '#', 'admin', NOW()),
  ('人员档案新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:worker:add',    '#', 'admin', NOW()),
  ('人员档案修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:worker:edit',   '#', 'admin', NOW()),
  ('人员档案删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:worker:remove', '#', 'admin', NOW()),
  ('人员档案导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:worker:export', '#', 'admin', NOW());

-- 子菜单2：角色规则
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
  ('角色规则', @worker_dir_id, 2, 'role', 'worker/role/index', '', '', 1, 0, 'C', '0', '0', 'worker:role:list', 'role', 'admin', NOW(), '人员角色与考勤/资质规则');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('角色规则查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:role:query',  '#', 'admin', NOW()),
  ('角色规则新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:role:add',    '#', 'admin', NOW()),
  ('角色规则修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:role:edit',   '#', 'admin', NOW()),
  ('角色规则删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:role:remove', '#', 'admin', NOW()),
  ('角色规则导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:role:export', '#', 'admin', NOW());

-- 子菜单3：资质证件
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
  ('资质证件', @worker_dir_id, 3, 'cert', 'worker/cert/index', '', '', 1, 0, 'C', '0', '0', 'worker:cert:list', 'documentation', 'admin', NOW(), '人员资质证件上传与审核');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('资质证件查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:cert:query',  '#', 'admin', NOW()),
  ('资质证件新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:cert:add',    '#', 'admin', NOW()),
  ('资质证件修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:cert:edit',   '#', 'admin', NOW()),
  ('资质证件删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:cert:remove', '#', 'admin', NOW()),
  ('资质证件导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:cert:export', '#', 'admin', NOW());

-- 子菜单4：人脸信息
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
  ('人脸信息', @worker_dir_id, 4, 'face', 'worker/face/index', '', '', 1, 0, 'C', '0', '0', 'worker:face:list', 'eye-open', 'admin', NOW(), '人员人脸采集（特征值后续 AI）');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('人脸信息查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:face:query',  '#', 'admin', NOW()),
  ('人脸信息新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:face:add',    '#', 'admin', NOW()),
  ('人脸信息修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:face:edit',   '#', 'admin', NOW()),
  ('人脸信息删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:face:remove', '#', 'admin', NOW()),
  ('人脸信息导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:face:export', '#', 'admin', NOW());

-- 子菜单5：审核记录
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
  ('审核记录', @worker_dir_id, 5, 'audit', 'worker/audit/index', '', '', 1, 0, 'C', '0', '0', 'worker:audit:list', 'list', 'admin', NOW(), '人员/资质 资料审核流水');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('审核记录查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:audit:query',  '#', 'admin', NOW()),
  ('审核记录新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:audit:add',    '#', 'admin', NOW()),
  ('审核记录修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:audit:edit',   '#', 'admin', NOW()),
  ('审核记录删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:audit:remove', '#', 'admin', NOW()),
  ('审核记录导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:audit:export', '#', 'admin', NOW());

-- 子菜单6：打卡记录
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
  ('打卡记录', @worker_dir_id, 6, 'checkin', 'worker/checkin/index', '', '', 1, 0, 'C', '0', '0', 'worker:checkin:list', 'time', 'admin', NOW(), '签到/签退/点到 打卡记录');
SET @m := LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
  ('打卡记录查询', @m, 1, '#', '', 1, 0, 'F', '0', '0', 'worker:checkin:query',  '#', 'admin', NOW()),
  ('打卡记录新增', @m, 2, '#', '', 1, 0, 'F', '0', '0', 'worker:checkin:add',    '#', 'admin', NOW()),
  ('打卡记录修改', @m, 3, '#', '', 1, 0, 'F', '0', '0', 'worker:checkin:edit',   '#', 'admin', NOW()),
  ('打卡记录删除', @m, 4, '#', '', 1, 0, 'F', '0', '0', 'worker:checkin:remove', '#', 'admin', NOW()),
  ('打卡记录导出', @m, 5, '#', '', 1, 0, 'F', '0', '0', 'worker:checkin:export', '#', 'admin', NOW());

-- ---------------------------------------------------------
-- 27.9 数据字典：类型（6 组）
-- ---------------------------------------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`) VALUES
  ('人员状态',     'worker_status',       '0', 'admin', NOW(), '施工人员在场/离场/禁用'),
  ('人员审核状态', 'worker_audit_status', '0', 'admin', NOW(), '人员/资质 审核状态'),
  ('人脸录入状态', 'worker_face_status',  '0', 'admin', NOW(), '是否已录入人脸'),
  ('证件类型',     'worker_cert_type',    '0', 'admin', NOW(), '资质证件类型'),
  ('单位类型',     'worker_unit_type',    '0', 'admin', NOW(), '管网/第三方/施工方'),
  ('打卡类型',     'worker_check_type',   '0', 'admin', NOW(), '签到/签退/点到');

-- ---------------------------------------------------------
-- 27.10 数据字典：明细（21 条）
-- ---------------------------------------------------------
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`) VALUES
  (1, '在场', '0', 'worker_status', 'success', 'Y', '0', 'admin', NOW()),
  (2, '离场', '1', 'worker_status', 'info',    'N', '0', 'admin', NOW()),
  (3, '禁用', '2', 'worker_status', 'danger',  'N', '0', 'admin', NOW()),
  (1, '待审核', '0', 'worker_audit_status', 'warning', 'Y', '0', 'admin', NOW()),
  (2, '已通过', '1', 'worker_audit_status', 'success', 'N', '0', 'admin', NOW()),
  (3, '已驳回', '2', 'worker_audit_status', 'danger',  'N', '0', 'admin', NOW()),
  (4, '已过期', '3', 'worker_audit_status', 'info',    'N', '0', 'admin', NOW()),
  (1, '未录入', '0', 'worker_face_status', 'info',    'Y', '0', 'admin', NOW()),
  (2, '已录入', '1', 'worker_face_status', 'success', 'N', '0', 'admin', NOW()),
  (1, '身份证',     'id_card',        'worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (2, '安全员证',   'safe_cert',      'worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (3, '电工证',     'electric_cert',  'worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (4, '监理证',     'supervisor_cert','worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (5, '作业监护证', 'guardian_cert',  'worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (6, '保险',       'insurance',      'worker_cert_type', 'default', 'N', '0', 'admin', NOW()),
  (1, '管网',   '1', 'worker_unit_type', 'default', 'N', '0', 'admin', NOW()),
  (2, '第三方', '2', 'worker_unit_type', 'default', 'N', '0', 'admin', NOW()),
  (3, '施工方', '3', 'worker_unit_type', 'default', 'N', '0', 'admin', NOW()),
  (1, '签到', '1', 'worker_check_type', 'success', 'N', '0', 'admin', NOW()),
  (2, '签退', '2', 'worker_check_type', 'info',    'N', '0', 'admin', NOW()),
  (3, '点到', '3', 'worker_check_type', 'warning', 'N', '0', 'admin', NOW());
