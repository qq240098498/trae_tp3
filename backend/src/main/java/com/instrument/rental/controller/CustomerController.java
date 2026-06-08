package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Customer;
import com.instrument.rental.service.CustomerService;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/list")
    public Result<Page<Customer>> list(PageQuery pageQuery) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageQuery.getKeyword())) {
            wrapper.and(w -> w.like(Customer::getName, pageQuery.getKeyword())
                    .or().like(Customer::getPhone, pageQuery.getKeyword())
                    .or().like(Customer::getIdCard, pageQuery.getKeyword()));
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = customerService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            return Result.fail("客户不存在");
        }
        return Result.ok(customer);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Customer customer) {
        customerService.save(customer);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Customer customer) {
        customerService.updateById(customer);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/all")
    public Result<List<Customer>> all() {
        List<Customer> list = customerService.list();
        return Result.ok(list);
    }
}
