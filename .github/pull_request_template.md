<!--
Title: type(scope): imperative subject, e.g. "feat(customer-service): add customer search".
It is checked by PR checks and becomes the release note: feat -> minor, other types -> patch.
Breaking change: add "!" after the scope, or the "breaking-change" label -> major.
-->

## Summary

<!-- One or two sentences: what this PR does and why. -->

## What has been done

<!-- The changes in detail, when the summary is not enough: services, endpoints, events, schemas, config. -->

## Testing

- [ ] Unit tests added or updated (happy path and at least one edge case)
- [ ] `./mvnw -B -ntp clean install` passes locally
- [ ] Manually checked (describe below)

<!-- How it was tested, and anything not covered. -->

## Notes

<!-- Risks, follow-ups, related docs. Link issues with "Closes #NN" so they close on merge. Delete if empty. -->

## Breaking changes

<!-- Only for breaking changes, otherwise delete this section: what breaks (API, events, config, schema) and how to migrate. Also add "!" to the title or the "breaking-change" label. -->
