# Backlog

## P0: V1 core

- [x] Bootstrap Java project and TamboUI.
- [x] Main screen with services/details/logs/status panels.
- [x] Keyboard service selection.
- [x] Clean quit.
- [x] Compose file discovery walking upward from cwd.
- [ ] Docker Compose preflight.
- [x] `compose config --format json` adapter.
- [x] Render real services.
- [x] `compose ps --all --format json` adapter.
- [x] Runtime/health mapping.
- [x] Manual refresh.
- [x] Periodic runtime refresh.
- [x] Up selected service.
- [ ] Stop selected service.
- [ ] Restart selected service.
- [ ] Operation state.
- [ ] Error notification.
- [ ] Selected service logs follow.
- [ ] Ring buffer.
- [ ] Log scrolling and follow/freeze.
- [ ] All-services logs mode.
- [ ] Safe log session switching.
- [ ] Up all.
- [ ] Stop all.
- [ ] Restart all.
- [ ] Panel resizing by keyboard.
- [ ] Narrow terminal layout.

## P1: hardening

- [ ] Compose events stream.
- [ ] Runtime refresh invalidation from events.
- [ ] Reconciliation polling fallback.
- [ ] Stats snapshots.
- [ ] CPU and memory in details.
- [ ] Debug logging.
- [ ] Better error details overlay.
- [ ] Integration tests with real Docker.
- [ ] Shutdown race tests.
- [ ] Handle service list changes after Compose edit.
- [ ] Help overlay.

## P2: useful follow-ups

- [ ] Filter services.
- [ ] Search logs.
- [ ] Copy log line.
- [ ] Show service dependencies.
- [ ] Profiles view.
- [ ] `up --build` action.
- [ ] recreate action.
- [ ] optional down action with confirmation.
- [ ] save layout ratios.
- [ ] mouse resize if worth it.
- [ ] events panel.

## P3: later

- [ ] Multiple registered workspaces.
- [ ] Docker Engine API adapter evaluation.
- [ ] Docker contexts UI.
- [ ] Kafka explorer module.
- [ ] Native executable packaging evaluation.
