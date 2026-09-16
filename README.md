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
