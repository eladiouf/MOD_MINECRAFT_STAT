# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.1.x   | Yes       |
| 1.0.x   | No        |

## Reporting a Vulnerability

To report a security vulnerability:

1. **DO NOT** open a public GitHub issue
2. Contact the maintainer directly via Discord or email
3. Include detailed steps to reproduce
4. Allow reasonable time for a fix before disclosure

## Critical Issues

Critical issues that should be reported:
- Arbitrary command execution
- Player data corruption/loss
- Server crash exploits
- Unauthorized privilege escalation
- Data leakage between players

## Best Practices for Servers

- Keep the mod updated to the latest version
- Restrict `/statmod` command to trusted admins (permission level 2+)
- Regular backups of player data (`/statmod backup`)
- Test config changes on a staging server first
