# UI mockups

## Wide

```text
┌ tambo-demo ──────────────────────────────────────────────────────────┐
│ Services                         │ Details                            │
│ > ● gateway       running        │ gateway                            │
│   ● api           running        │ runtime       running              │
│   ● worker        running        │ health        not configured       │
│   ✕ failing       exited (7)     │ image         nginx:alpine         │
│                                  │ ports         8080 -> 80            │
│                                  │ containers    1                     │
├──────────────────────────────────┴────────────────────────────────────┤
│ Logs [selected: gateway]                                follow: on   │
│ gateway-1 | 2026-09-17T19:00:00Z ...                                │
│ gateway-1 | 2026-09-17T19:00:01Z ...                                │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│ u up  s stop  r restart  l logs  Tab focus  ? help  q quit           │
└───────────────────────────────────────────────────────────────────────┘
```

## All logs

```text
│ Logs [all services]                                      follow: on  │
│ api-1     | api heartbeat                                            │
│ worker-1  | worker tick                                              │
│ gateway-1 | GET / 200                                                │
```

## Narrow

```text
┌ Services ───────────────┐
│ > gateway      running  │
│   api          running  │
│   worker       running  │
├ Details ────────────────┤
│ gateway                 │
│ healthy                 │
│ 8080 -> 80              │
├ Logs ───────────────────┤
│ ...                     │
│ ...                     │
├─────────────────────────┤
│ u/s/r  l logs  q quit   │
└─────────────────────────┘
```

## Operation feedback

```text
> ● challenge   running    [restarting...]
```

Runtime stays based on observation. The operation badge is local state.

## Error

```text
┌ Error ────────────────────────────────────────────────────────────────┐
│ Failed to restart challenge                                          │
│ docker compose exited with code 1                                    │
│                                                                      │
│ no container found for service "challenge"                           │
│                                                                      │
│ [Esc] close                                                          │
└───────────────────────────────────────────────────────────────────────┘
```
