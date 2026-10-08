package com.app.controller;

import com.app.dto.OrderLookupDto;
import com.app.service.InvoiceLookupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lookup")
public class LookupController {

    private final InvoiceLookupService service;

    public LookupController(InvoiceLookupService s) {
        this.service = s;
    }

    @GetMapping("/order/{code}")
    public OrderLookupDto one(@PathVariable String code) {
        return service.lookup(code);
    }

    /** POST body: {"codes": ["23314", "23315", "NĐ-03054"]} */
    @PostMapping("/orders")
    public Map<String, OrderLookupDto> many(@RequestBody Map<String, List<String>> body) {
        return service.lookupMany(body.getOrDefault("codes", List.of()));
    }
}