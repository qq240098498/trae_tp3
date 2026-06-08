package com.instrument.rental.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.mapper.CustomerMapper;
import com.instrument.rental.service.CustomerService;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {
}
