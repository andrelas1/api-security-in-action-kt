package com.natter.api.controller

import com.natter.api.core.DatabaseService
import com.natter.api.model.Space
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.validation.annotation.Validated
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@RestController
@RequestMapping("/api/spaces", produces = ["application/json"])
@Validated
class SpaceController(private val databaseService: DatabaseService) {

    @GetMapping
    fun getSpaces(): List<Space> {
        val spaces =
                databaseService.db.findAll(
                        Space::class.java,
                        "SELECT space_id, name, owner FROM spaces"
                )

        return spaces
    }

    @PostMapping
    fun createSpace(@Valid @RequestBody createSpaceRequest: CreateSpaceRequest): ResponseEntity<Space> {
        val space = databaseService.createSpace(createSpaceRequest.name, createSpaceRequest.owner)
        return ResponseEntity.status(HttpStatus.CREATED).body(space)
    }

    data class CreateSpaceRequest(
        @field:NotBlank(message = "Space name cannot be empty")
        @field:Size(max = 255, message = "Space name must be shorter than 255 characters")
        val name: String,
        
        @field:NotBlank(message = "Owner cannot be empty")
        @field:Pattern(
            regexp = "[a-zA-Z][a-zA-Z0-9]{1,29}",
            message = "Owner must start with a letter and be 2-30 characters long, containing only letters and numbers"
        )
        val owner: String
    )
}
