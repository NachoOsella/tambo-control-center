# Backlog

## P0: V1 core

- [x] Bootstrap Java project and TamboUI.
- [x] Main screen with services/details/logs/status panels.
- [x] Keyboard service selection.
- [x] Clean quit.
- [x] Compose file discovery walking upward from cwd.
- [x] Docker Compose preflight.
- [x] `compose config --format json` adapter.
- [x] Render real services.
- [x] `compose ps --all --format json` adapter.
- [x] Runtime/health mapping.
- [x] Manual refresh.
- [x] Periodic runtime refresh.
- [x] Up selected service.
- [x] Stop selected service.
- [x] Restart selected service.
- [x] Operation state.
- [x] Error notification.
- [x] Selected service logs follow.
- [x] Ring buffer.
- [x] Log scrolling and follow/freeze.
- [x] All-services logs mode.
- [x] Safe log session switching.
- [x] Up all.
- [x] Stop all.
- [x] Restart all.
- [x] Panel resizing by keyboard.
- [x] Narrow terminal layout.

## P1: hardening

- [x] Compose events stream.
- [x] Runtime refresh invalidation from events.
- [x] Reconciliation polling fallback.
- [x] Stats snapshots.
- [x] CPU and memory in details.
- [x] Debug logging.
- [x] Better error details overlay.
- [x] Integration tests with real Docker.
- [x] Shutdown race tests.
- [ ] Handle service list changes after Compose edit.
- [x] Detect Compose file changes and warn before using stale services.
- [x] Help overlay.

## P2: useful follow-ups

- [x] Filter services.
- [ ] Search logs.
- [ ] Copy log line.
- [x] Show service dependencies.
- [x] Profiles view.
- [x] `up --build` action.
- [x] recreate action.
- [x] optional down action with confirmation.
- [ ] save layout ratios.
- [ ] mouse resize if worth it.
- [ ] events panel.

## P3: later

- [ ] Multiple registered workspaces.
- [ ] Docker Engine API adapter evaluation.
- [ ] Docker contexts UI.
- [ ] Kafka explorer module.
- [ ] Native executable packaging evaluation.
