INSERT IGNORE INTO customer (id, name, phone, id_card, address, email, remark) VALUES
(1, '张三', '13800138001', '110101199001011234', '北京市朝阳区建国路88号', 'zhangsan@email.com', '老客户'),
(2, '李四', '13800138002', '110101199002021234', '北京市海淀区中关村大街1号', 'lisi@email.com', '学生客户'),
(3, '王五', '13800138003', '110101199003031234', '北京市东城区王府井大街2号', 'wangwu@email.com', NULL);

INSERT IGNORE INTO instrument (id, name, brand, category, model, serial_no, purchase_price, daily_rent, deposit_amount, status, `condition`, remark) VALUES
(1, '雅马哈钢琴', 'Yamaha', '键盘', 'U1', 'YMH-U1-001', 35000.00, 80.00, 5000.00, 'AVAILABLE', 'NEW', '全新立式钢琴'),
(2, '泰勒吉他', 'Taylor', '弦乐', '214ce', 'TYL-214-001', 12000.00, 30.00, 2000.00, 'AVAILABLE', 'GOOD', '民谣吉他'),
(3, '雅马哈小号', 'Yamaha', '管乐', 'YTR-2330', 'YMH-TR-001', 4500.00, 20.00, 1000.00, 'RENTED', 'GOOD', NULL),
(4, '罗兰电鼓', 'Roland', '打击', 'TD-07KV', 'RLD-TD-001', 8000.00, 40.00, 2000.00, 'AVAILABLE', 'NEW', '电子鼓套装'),
(5, '雅马哈萨克斯', 'Yamaha', '管乐', 'YAS-280', 'YMH-SX-001', 6000.00, 25.00, 1500.00, 'MAINTENANCE', 'FAIR', '需要调音');

INSERT IGNORE INTO rental_order (id, order_no, customer_id, instrument_id, start_date, end_date, daily_rent, total_rent, deposit_amount, status, remark) VALUES
(1, 'RL20260601001', 1, 3, '2026-06-01', '2026-07-01', 20.00, 600.00, 1000.00, 'ACTIVE', '正常租赁中');

INSERT IGNORE INTO deposit_record (id, order_id, type, amount, pay_method, status, remark) VALUES
(1, 1, 'COLLECT', 1000.00, 'WECHAT', 'COMPLETED', '收取押金');

INSERT IGNORE INTO points_config (id, config_key, config_name, config_value, description) VALUES
(1, 'EARN_RATE', '消费积分获得比例', 1.00, '每消费1元可获得的积分数'),
(2, 'DEDUCT_RATE', '积分抵扣现金比例', 100.00, '每100积分可抵扣1元现金'),
(3, 'MAX_DEDUCT_PERCENT', '积分抵扣最高百分比', 30.00, '积分抵扣金额最高不超过订单金额的30%');

INSERT IGNORE INTO customer_points (id, customer_id, total_points, available_points, used_points, expired_points) VALUES
(1, 1, 0, 0, 0, 0),
(2, 2, 0, 0, 0, 0),
(3, 3, 0, 0, 0, 0);

INSERT IGNORE INTO coupon_template (id, name, type, discount_value, min_amount, max_discount_amount, valid_days, points_compatible, total_count, description) VALUES
(1, '新人满100减20券', 'FIXED', 20.00, 100.00, NULL, 30, 1, 100, '新用户专享，订单满100元可用，有效期30天，可与积分组合使用'),
(2, '9折优惠券', 'PERCENT', 9.00, 50.00, 100.00, 15, 1, 200, '全场9折，最低消费50元，最高优惠100元，有效期15天'),
(3, '满500减100大额券', 'FIXED', 100.00, 500.00, NULL, 60, 0, 50, '满500元立减100元，有效期60天，与积分抵扣互斥，不可同时使用'),
(4, '8.5折乐器租赁券', 'PERCENT', 8.50, 200.00, 200.00, 45, 1, -1, '8.5折优惠，最低消费200元，最高优惠200元，不限量');
