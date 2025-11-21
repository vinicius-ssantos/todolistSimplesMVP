package com.viniss.todo.it

import org.springframework.context.annotation.Import

/**
 * H2 implementation of EmailServiceIT.
 *
 * Runs all email service integration tests against H2 in-memory database.
 */
@Import(EmailServiceIT.TestEmailConfiguration::class)
class EmailServiceH2IT : EmailServiceIT()
