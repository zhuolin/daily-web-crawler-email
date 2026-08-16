# Daily Web Crawler with Email Notification

Java 17 + Jsoup + Jakarta Mail crawler for GitHub Actions.

Default example:
- URL: https://www.fedcourt.gov.au/court-calendar/daily-court-lists/vic
- Search name: Alleyway

## GitHub configuration

Repository Variables:
- `CRAWL_URL` = `https://www.fedcourt.gov.au/court-calendar/daily-court-lists/vic`
- `SEARCH_NAMES` = `Alleyway`

Multiple names can be separated with `|`, e.g. `Alleyway|John Smith|ABC Pty Ltd`.

Repository Secrets:
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `EMAIL_FROM`
- `EMAIL_TO`

For Gmail, use an App Password where applicable, not your normal password.

The workflow runs daily at 08:00 UTC. It also supports **Actions -> Daily Web Crawler -> Run workflow**, where URL and names can be entered for a one-off test.

An email is sent only when at least one name is found. The email contains the URL, UTC check time, matched name(s), and surrounding page text.

## Local run

Set the same environment variables and run:

`./gradlew run`

Or supply URL/names directly:

`./gradlew run --args='--url=https://example.com/page --names=Alleyway'`

SMTP credentials remain environment variables.

## Notes

Jsoup works when the relevant content is present in the server-returned HTML. If a site renders the data only after JavaScript executes, use its underlying API or browser automation instead.

Check the target site's robots.txt, terms of use, and applicable crawling restrictions before automating requests.
