/** Fail a release gate when a named JUnit suite is absent, incomplete or skipped. */
import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'

const expected = Number(process.argv[2])
const name = process.argv[3]
assert.ok(Number.isInteger(expected) && expected > 0 && name, 'Usage: node assert-surefire.mjs COUNT ClassName')
const files = readdirSync('target/surefire-reports').filter(file => file.startsWith('TEST-') && file.endsWith(`.${name}.xml`))
assert.equal(files.length, 1, `Expected exactly one Surefire report for ${name}`)
const xml = readFileSync(`target/surefire-reports/${files[0]}`, 'utf8')
const count = expression => [...xml.matchAll(expression)].length
const tests = count(/<testcase(?:\s|>)/g)
const skipped = count(/<skipped(?:\s|\/>|>)/g)
const failures = count(/<(?:failure|error)(?:\s|\/>|>)/g)
console.log(`${name}: tests=${tests}, skipped=${skipped}, failures=${failures}`)
assert.equal(tests, expected)
assert.equal(skipped, 0)
assert.equal(failures, 0)
