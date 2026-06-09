CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '客户姓名',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    id_card VARCHAR(18) COMMENT '身份证号',
    address VARCHAR(200) COMMENT '地址',
    email VARCHAR(100) COMMENT '邮箱',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '客户表';

CREATE TABLE IF NOT EXISTS instrument (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '乐器名称',
    brand VARCHAR(50) COMMENT '品牌',
    category VARCHAR(30) COMMENT '分类(键盘/弦乐/管乐/打击/其他)',
    model VARCHAR(50) COMMENT '型号',
    serial_no VARCHAR(50) COMMENT '序列号',
    purchase_price DECIMAL(10,2) COMMENT '采购价格',
    daily_rent DECIMAL(10,2) NOT NULL COMMENT '日租金',
    deposit_amount DECIMAL(10,2) NOT NULL COMMENT '押金金额',
    status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态(AVAILABLE/RENTED/MAINTENANCE/RETIRED)',
    `condition` VARCHAR(20) DEFAULT 'NEW' COMMENT '成色(NEW/GOOD/FAIR/POOR)',
    image_url VARCHAR(500) COMMENT '图片URL',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '乐器表';

CREATE TABLE IF NOT EXISTS rental_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '订单编号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    instrument_id BIGINT NOT NULL COMMENT '乐器ID',
    start_date DATE NOT NULL COMMENT '租赁开始日期',
    end_date DATE NOT NULL COMMENT '租赁结束日期',
    daily_rent DECIMAL(10,2) NOT NULL COMMENT '日租金',
    total_rent DECIMAL(10,2) NOT NULL COMMENT '总租金',
    deposit_amount DECIMAL(10,2) NOT NULL COMMENT '押金金额',
    points_deduct_amount DECIMAL(10,2) DEFAULT 0 COMMENT '积分抵扣金额',
    used_points INT DEFAULT 0 COMMENT '使用积分数',
    coupon_id BIGINT COMMENT '使用优惠券ID',
    coupon_deduct_amount DECIMAL(10,2) DEFAULT 0 COMMENT '优惠券抵扣金额',
    actual_pay_amount DECIMAL(10,2) DEFAULT 0 COMMENT '实际支付金额',
    earned_points INT DEFAULT 0 COMMENT '获得积分数',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/RETURNED/OVERDUE/CANCELLED)',
    actual_return_date DATE COMMENT '实际归还日期',
    overdue_fee DECIMAL(10,2) DEFAULT 0 COMMENT '逾期费用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '租赁订单表';

CREATE TABLE IF NOT EXISTS deposit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    type VARCHAR(20) NOT NULL COMMENT '类型(COLLECT/REFUND/DEDUCT)',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    pay_method VARCHAR(20) COMMENT '支付方式(CASH/WECHAT/ALIPAY/BANK)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/COMPLETED/CANCELLED)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '押金记录表';

CREATE TABLE IF NOT EXISTS renewal_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    original_end_date DATE NOT NULL COMMENT '原到期日期',
    new_end_date DATE NOT NULL COMMENT '新到期日期',
    additional_rent DECIMAL(10,2) NOT NULL COMMENT '追加租金',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '续租记录表';

CREATE TABLE IF NOT EXISTS maintenance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instrument_id BIGINT NOT NULL COMMENT '乐器ID',
    type VARCHAR(30) NOT NULL COMMENT '维保类型(日常保养/维修/调音/更换配件)',
    description VARCHAR(500) COMMENT '描述',
    cost VARCHAR(50) COMMENT '费用',
    maintenance_date DATETIME NOT NULL COMMENT '维保日期',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/IN_PROGRESS/COMPLETED)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '维保记录表';

CREATE TABLE IF NOT EXISTS reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    expire_date DATE NOT NULL COMMENT '到期日期',
    days_before_expire INT DEFAULT 3 COMMENT '提前几天提醒',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/SENT/IGNORED)',
    notify_method VARCHAR(20) DEFAULT 'SYSTEM' COMMENT '通知方式(SYSTEM/SMS/PHONE)',
    notify_time DATETIME COMMENT '通知时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '到期提醒表';

CREATE TABLE IF NOT EXISTS damage_registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT COMMENT '关联订单ID',
    instrument_id BIGINT NOT NULL COMMENT '乐器ID',
    customer_id BIGINT COMMENT '客户ID',
    damage_type VARCHAR(30) NOT NULL COMMENT '损坏类型(外观损坏/功能故障/配件丢失/音质异常/其他)',
    description VARCHAR(500) COMMENT '损坏描述',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度(MINOR/MODERATE/SEVERE)',
    estimated_cost DECIMAL(10,2) DEFAULT 0 COMMENT '预估维修费用',
    status VARCHAR(20) DEFAULT 'REPORTED' COMMENT '状态(REPORTED/REPAIR_CREATED/REPAIRED)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '损坏登记表';

CREATE TABLE IF NOT EXISTS repair_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '工单编号',
    damage_id BIGINT COMMENT '关联损坏登记ID',
    instrument_id BIGINT NOT NULL COMMENT '乐器ID',
    repair_type VARCHAR(30) NOT NULL COMMENT '维修类型(外观修复/功能维修/配件更换/调音校正/其他)',
    description VARCHAR(500) COMMENT '维修描述',
    estimated_cost DECIMAL(10,2) DEFAULT 0 COMMENT '预估费用',
    actual_cost DECIMAL(10,2) COMMENT '实际费用',
    assignee VARCHAR(50) COMMENT '负责人',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/IN_PROGRESS/COMPLETED)',
    maintenance_record_id BIGINT COMMENT '关联维保记录ID',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '维修工单表';

CREATE TABLE IF NOT EXISTS points_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_value DECIMAL(10,2) NOT NULL COMMENT '配置值',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '积分配置表';

CREATE TABLE IF NOT EXISTS customer_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE COMMENT '客户ID',
    total_points INT DEFAULT 0 COMMENT '累计获得积分',
    available_points INT DEFAULT 0 COMMENT '可用积分',
    used_points INT DEFAULT 0 COMMENT '已使用积分',
    expired_points INT DEFAULT 0 COMMENT '已过期积分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '客户积分账户表';

CREATE TABLE IF NOT EXISTS points_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    order_id BIGINT COMMENT '关联订单ID',
    type VARCHAR(30) NOT NULL COMMENT '类型(EARN/DEDUCT/MANUAL_ADD/MANUAL_DEDUCT/EXPIRE)',
    points INT NOT NULL COMMENT '积分变动(正数增加,负数减少)',
    related_amount DECIMAL(10,2) COMMENT '关联金额',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_customer_id (customer_id),
    INDEX idx_order_id (order_id)
) COMMENT '积分记录表';

CREATE TABLE IF NOT EXISTS coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type VARCHAR(20) NOT NULL COMMENT '类型(FIXED满减/PERCENT折扣)',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠值(满减金额或折扣比例)',
    min_amount DECIMAL(10,2) DEFAULT 0 COMMENT '最低使用金额',
    max_discount_amount DECIMAL(10,2) COMMENT '最大优惠金额(折扣券用)',
    valid_days INT COMMENT '有效天数(从发放日起算)',
    valid_start_date DATE COMMENT '固定有效期开始',
    valid_end_date DATE COMMENT '固定有效期结束',
    points_compatible TINYINT(1) DEFAULT 1 COMMENT '是否可与积分组合使用(1是/0否互斥)',
    total_count INT DEFAULT -1 COMMENT '发放总量(-1不限)',
    used_count INT DEFAULT 0 COMMENT '已使用数量',
    issued_count INT DEFAULT 0 COMMENT '已发放数量',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE/EXPIRED)',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '优惠券模板表';

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_no VARCHAR(50) NOT NULL UNIQUE COMMENT '优惠券编号',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    type VARCHAR(20) NOT NULL COMMENT '类型(FIXED满减/PERCENT折扣)',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '优惠值',
    min_amount DECIMAL(10,2) DEFAULT 0 COMMENT '最低使用金额',
    max_discount_amount DECIMAL(10,2) COMMENT '最大优惠金额',
    points_compatible TINYINT(1) DEFAULT 1 COMMENT '是否可与积分组合使用',
    status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态(AVAILABLE/USED/EXPIRED)',
    valid_start_date DATE NOT NULL COMMENT '有效期开始',
    valid_end_date DATE NOT NULL COMMENT '有效期结束',
    order_id BIGINT COMMENT '使用订单ID',
    used_time DATETIME COMMENT '使用时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_customer_id (customer_id),
    INDEX idx_template_id (template_id),
    INDEX idx_status (status)
) COMMENT '用户优惠券表';

CREATE TABLE IF NOT EXISTS coupon_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    template_id BIGINT COMMENT '模板ID',
    order_id BIGINT COMMENT '关联订单ID',
    type VARCHAR(30) NOT NULL COMMENT '类型(ISSUE发放/USE使用/EXPIRE过期/REVOKE撤回)',
    discount_amount DECIMAL(10,2) COMMENT '优惠金额',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_coupon_id (coupon_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_order_id (order_id)
) COMMENT '优惠券流水记录表';

CREATE TABLE IF NOT EXISTS work_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_type VARCHAR(50) NOT NULL COMMENT '流程类型(RENTAL_CREATE/RENTAL_RENEW/RENTAL_RETURN/COUPON_ISSUE/COUPON_USE/COUPON_REVOKE/DEPOSIT_COLLECT/DEPOSIT_REFUND/DEPOSIT_DEDUCT/POINTS_DEDUCT/POINTS_EARN)',
    order_id BIGINT COMMENT '订单ID',
    order_no VARCHAR(30) COMMENT '订单编号',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(50) COMMENT '客户姓名',
    instrument_id BIGINT COMMENT '乐器ID',
    instrument_name VARCHAR(100) COMMENT '乐器名称',
    coupon_id BIGINT COMMENT '优惠券ID',
    coupon_no VARCHAR(50) COMMENT '优惠券编号',
    template_id BIGINT COMMENT '优惠券模板ID',
    template_name VARCHAR(100) COMMENT '优惠券模板名称',
    amount DECIMAL(10,2) DEFAULT 0 COMMENT '金额',
    use_discount TINYINT(1) DEFAULT 0 COMMENT '是否使用优惠',
    discount_type VARCHAR(20) COMMENT '优惠方式(COUPON/POINTS/BOTH)',
    coupon_deduct_amount DECIMAL(10,2) DEFAULT 0 COMMENT '优惠券抵扣金额',
    points_deduct_amount DECIMAL(10,2) DEFAULT 0 COMMENT '积分抵扣金额',
    used_points INT DEFAULT 0 COMMENT '使用或获得的积分数',
    pay_method VARCHAR(20) COMMENT '支付方式',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_process_type (process_type),
    INDEX idx_order_id (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_operator (operator),
    INDEX idx_create_time (create_time)
) COMMENT '工作日志表';
