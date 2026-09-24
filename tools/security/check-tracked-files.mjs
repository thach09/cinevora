#!/usr/bin/env node
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

const files = execFileSync('git', ['ls-files', '-z'], { encoding: 'utf8' })
  .split('\0')
  .filter(Boolean)

const forbiddenPath = /(^|\/)(?:\.tmp|uploads|legacy-cli\/exports|test-results(?:-[^/]+)?|playwright-report)(?:\/|$)/i
const forbiddenSecret = /(?:AKIA|ASIA)[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|(?:ghp|github_pat|xox[baprs])_[A-Za-z0-9_-]{16,}|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}/
const violations = []

for (const file of files) {
  if (forbiddenPath.test(file)) violations.push(`tracked generated/sensitive path: ${file}`)
  let content
  try {
    content = readFileSync(file, 'utf8')
  } catch {
    continue
  }
  if (forbiddenSecret.test(content)) violations.push(`credential-like material in: ${file}`)
}

if (violations.length) {
  console.error('Tracked-file security gate failed:')
  for (const violation of violations) console.error(`- ${violation}`)
  process.exit(1)
}

console.log(`Tracked-file security gate passed (${files.length} files scanned)`)
