package com.instrument.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.rental.common.PageQuery;
import com.instrument.rental.common.Result;
import com.instrument.rental.entity.Instrument;
import com.instrument.rental.service.InstrumentService;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instrument")
public class InstrumentController {

    @Autowired
    private InstrumentService instrumentService;

    @GetMapping("/list")
    public Result<Page<Instrument>> list(PageQuery pageQuery,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Instrument> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(pageQuery.getKeyword())) {
            wrapper.and(w -> w.like(Instrument::getName, pageQuery.getKeyword())
                    .or().like(Instrument::getBrand, pageQuery.getKeyword())
                    .or().like(Instrument::getSerialNo, pageQuery.getKeyword()));
        }
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Instrument::getCategory, category);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Instrument::getStatus, status);
        }
        wrapper.orderByDesc(Instrument::getCreateTime);
        Page<Instrument> page = instrumentService.page(pageQuery.toPage(), wrapper);
        return Result.ok(page);
    }

    @GetMapping("/{id}")
    public Result<Instrument> getById(@PathVariable Long id) {
        Instrument instrument = instrumentService.getById(id);
        if (instrument == null) {
            return Result.fail("乐器不存在");
        }
        return Result.ok(instrument);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Instrument instrument) {
        instrumentService.save(instrument);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Instrument instrument) {
        instrumentService.updateById(instrument);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        instrumentService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/available")
    public Result<List<Instrument>> available() {
        LambdaQueryWrapper<Instrument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Instrument::getStatus, "AVAILABLE");
        List<Instrument> list = instrumentService.list(wrapper);
        return Result.ok(list);
    }
}
