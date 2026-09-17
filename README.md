# Beneficiary Credit Scoring and Loan Management System

This repository contains a full-stack Java and React college project for beneficiary credit scoring and loan management.

## Stack
- Backend: Java 25 LTS, Spring Boot 4, Maven, JDBC
- Frontend: React, Vite, Bootstrap, React Router, Axios
- Database: MySQL

## Project Goal
Build a production-style loan management workflow covering beneficiary management, credit scoring, applications, approval, repayment tracking, EMI calculation, reports, and audit logs.

## Day 1 Scope
- Initialize Git repository and GitHub remote
- Scaffold backend and frontend projects
- Configure project structure and base dependencies
- Prepare a clean, runnable workspace for the next development day

## Day 2 Scope
- Add beneficiary CRUD API at `/api/beneficiaries`
- Validate required beneficiary fields and return consistent 400/404 errors
- Cover create, read, list, update, and delete flows with integration tests

### Beneficiary API
- `POST /api/beneficiaries` creates a beneficiary
- `GET /api/beneficiaries` lists beneficiaries
- `GET /api/beneficiaries/{id}` retrieves one beneficiary
- `PUT /api/beneficiaries/{id}` updates a beneficiary
- `DELETE /api/beneficiaries/{id}` removes a beneficiary

## Day 3 Scope
- Add loan application CRUD API at `/api/loans` and `/api/loan-applications`
- Validate applicant, amount, term, and purpose before accepting a loan request
- Return consistent 400/404 responses for invalid or missing loan applications
- Cover create, read, list, update, and delete flows with integration tests

### Loan Application API
- `POST /api/loans` and `POST /api/loan-applications` creates a loan request
- `GET /api/loans` and `GET /api/loan-applications` lists loans
- `GET /api/loans/{id}` and `GET /api/loan-applications/{id}` retrieves one loan
- `PUT /api/loans/{id}` and `PUT /api/loan-applications/{id}` updates a loan
- `DELETE /api/loans/{id}` and `DELETE /api/loan-applications/{id}` removes a loan
