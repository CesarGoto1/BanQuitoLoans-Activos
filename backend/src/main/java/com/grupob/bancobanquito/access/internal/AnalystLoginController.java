package com.grupob.bancobanquito.access.internal;

import com.grupob.bancobanquito.access.AnalystService;
import com.grupob.bancobanquito.access.AnalystSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyst-sessions")
class AnalystLoginController {

    private final AnalystService analystService;

    AnalystLoginController(AnalystService analystService) {
        this.analystService = analystService;
    }

    @PostMapping
    AnalystSession login(@Valid @RequestBody AnalystLoginRequest request) {
        return analystService.login(request.nationalId());
    }
}
