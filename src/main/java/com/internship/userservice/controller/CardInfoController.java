package com.internship.userservice.controller;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.service.CardInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
public class CardInfoController {

    private final CardInfoService cardService;

    @PostMapping
    public ResponseEntity<CardInfoResponse> create(@Valid @RequestBody CardInfoRequest dto) {
        CardInfoResponse saved = cardService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardInfoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping
    public ResponseEntity<List<CardInfoResponse>> getByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(cardService.getAllByIds(ids));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<CardInfoResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardInfoResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CardInfoRequest dto) {
        return ResponseEntity.ok(cardService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}