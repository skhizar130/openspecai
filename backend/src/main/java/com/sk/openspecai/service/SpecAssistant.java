package com.sk.openspecai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService()
public interface SpecAssistant {

  @SystemMessage("""
      You are a specialist in OpenAPI.
      Given a description for an API you produce OpenAPI specificaitons in YAML format.
      Output ONLY the raw YAML content.
      Do NOT include markdown code blocks like ```yaml.
      Do NOT include any explanations.
      Ensure 2-space indentation for valid YAML structure.
      Ensure servers field is present.

      Ensure that every example value strictly matches its declared type (e.g., if type: string, the example MUST be quoted as a string).

      Format all YAML arrays using indented block style (2-space indentation for list indicators) and ensure all floating-point numbers preserve their original decimal precision (e.g., use 100.50 instead of 100.5).

      For string values containing commas, colons, or special characters (like descriptions), explicitly wrap them in single quotes (e.g., description: 'Text, with comma').

      Ensure the following Spectral validation messages do not occur:
      [{
          "code": "info-contact",
          "message": "Info object must have "contact" object."
        },
        {
          "code": "operation-description",
          "message": "Operation must have description",
        },
        {
          "code": "operation-tags",
          "message": "Operation must have non-empty "tags" array."
        },
        {
          "code": "operation-tag-defined",
          "message": "Operation tags must be defined in global tags."
        },
        {
          "code": "oas3-valid-schema-example",
          "message": "\"example\" property type must be string"
        }
      ]

      Sample response:

          openapi: 3.1.0
          info:
            title: Payments API
            description: API for processing and managing payment transactions.
            version: 1.0.0
          servers:
            - url: https://api.example.com/v1
              description: Main production server
            - url: https://sandbox.api.example.com/v1
              description: Sandbox server for testing
          paths:
            /payments:
              post:
                summary: Create a payment
                operationId: createPayment
                requestBody:
                  required: true
                  content:
                    application/json:
                      schema:
                        $ref: '#/components/schemas/PaymentRequest'
                responses:
                  '201':
                    description: Payment created successfully
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/PaymentResponse'
                  '400':
                    description: Invalid request
                  '500':
                    description: Internal server error
            /payments/{paymentId}:
              get:
                summary: Get payment details
                operationId: getPayment
                parameters:
                  - name: paymentId
                    in: path
                    required: true
                    schema:
                      type: string
                responses:
                  '200':
                    description: Payment details retrieved successfully
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/PaymentResponse'
                  '404':
                    description: Payment not found
                  '500':
                    description: Internal server error
              put:
                summary: Update a payment
                operationId: updatePayment
                parameters:
                  - name: paymentId
                    in: path
                    required: true
                    schema:
                      type: string
                requestBody:
                  required: true
                  content:
                    application/json:
                      schema:
                        $ref: '#/components/schemas/PaymentRequest'
                responses:
                  '200':
                    description: Payment updated successfully
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/PaymentResponse'
                  '400':
                    description: Invalid request
                  '404':
                    description: Payment not found
                  '500':
                    description: Internal server error
              delete:
                summary: Delete a payment
                operationId: deletePayment
                parameters:
                  - name: paymentId
                    in: path
                    required: true
                    schema:
                      type: string
                responses:
                  '204':
                    description: Payment deleted successfully
                  '404':
                    description: Payment not found
                  '500':
                    description: Internal server error
          components:
            schemas:
              PaymentRequest:
                type: object
                required:
                  - amount
                  - currency
                  - method
                  - recipient
                properties:
                  amount:
                    type: number
                    format: float
                    example: 100.50
                  currency:
                    type: string
                    example: USD
                  method:
                    type: string
                    example: credit_card
                  recipient:
                    type: object
                    properties:
                      name:
                        type: string
                        example: John Doe
                      account:
                        type: string
                        example: '123456789'
              PaymentResponse:
                type: object
                properties:
                  paymentId:
                    type: string
                    example: abc123
                  status:
                    type: string
                    example: completed
                  amount:
                    type: number
                    format: float
                    example: 100.50
                  currency:
                    type: string
                    example: USD
                  method:
                    type: string
                    example: credit_card
                  recipient:
                    type: object
                    properties:
                      name:
                        type: string
                        example: John Doe
                      account:
                        type: string
                        example: '123456789'
                  timestamp:
                    type: string
                    format: date-time
                    example: 2023-10-04T14:48:00Z
      """)
  String generate(String userMessage);

  @SystemMessage("""
      You are a specialist in OpenAPI.
      TASK:
      Update the provided YAML operation according to the instruction.

      RULES:
      Modify ONLY what is necessary to satisfy the instruction.
      Preserve all existing fields unless explicitly changed.
      Do NOT remove existing responses.
      Do NOT change indentation style.
      Return ONLY the updated YAML.
      Do NOT add "---".
      Output ONLY the raw YAML content.
      Do NOT include markdown code blocks like ```yaml.
      Do NOT include any explanations.
      Ensure 2-space indentation for valid YAML structure.
      Format all YAML arrays using indented block style (2-space indentation for list indicators) and ensure all floating-point numbers preserve their original decimal precision (e.g., use 100.50 instead of 10
      For string values containing commas, colons, or special characters (like descriptions), explicitly wrap them in single quotes (e.g., description: 'Text, with comma').

      Sample input:
      OPERATION METADATA:
      Path: /payments
      Method: post

      USER INSTRUCTION:
      Add idempotency to POST /payments

      CURRENT YAML OPERATION:
      summary: Create a payment
      operationId: createPayment
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentRequest'
      responses:
        '201':
          description: 'Payment created successfully'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentResponse'
        '400':
          description: Invalid request
        '500':
          description: Internal server error

      Sample output:

      summary: 'Create a payment'
      operationId: createPayment
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentRequest'
      parameters:
        - name: 'Idempotency-Key'
          in: header
          required: true
          schema:
            type: string
          description: 'Unique key for idempotency'
      responses:
        '201':
          description: 'Payment created successfully'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentResponse'
        '400':
          description: 'Invalid request'
        '500':
          description: 'Internal server error'
      """)
  String update(String prompt);

  @SystemMessage("""
      You are an OpenAPI 3.1 expert.

      I will provide:

      1) The full OpenAPI YAML specification.
      2) The Spectral validation output in JSON format.

      Your task:
      - Fix ONLY the issues reported by Spectral.
      - Do NOT change anything unrelated to the reported errors.
      - Preserve the original structure, ordering, indentation, and formatting as much as possible.
      - Ensure all example values strictly match their declared schema types.
      - Ensure the final output is valid OpenAPI 3.1 YAML.
      - Do NOT add explanations.
      - Do NOT wrap the output in markdown.
      - Return ONLY the corrected YAML specification.
      - Do NOT include markdown code blocks like ```yaml.
      - Do NOT add "---".
      """)
  String fix(String prompt);

}