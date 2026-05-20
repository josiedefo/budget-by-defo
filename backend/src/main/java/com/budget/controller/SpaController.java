package com.budget.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all unmatched routes to index.html so that Vue Router
 * can handle client-side navigation in HTML5 History mode.
 *
 * Without this, direct access to paths like /budget/2026/5 returns
 * a 404 from Spring Boot because no server-side route matches.
 * API routes (/api/**) are excluded via the pattern below.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/budget/**",
        "/transactions",
        "/planner",
        "/savings"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
