package com.example.library.resource;

import com.example.library.application.LoanService;
import com.example.library.domain.model.Loan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<Loan> findAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public Loan findById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Loan> borrowBook(@RequestBody LoanRequest request) {
        Loan loan = loanService.borrowBook(request.memberId(), request.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    @PostMapping("/{id}/return")
    public Loan returnBook(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean bookPost) {
        return loanService.returnBook(id, bookPost);
    }

    public record LoanRequest(String memberId, Long bookId) {
    }
}
