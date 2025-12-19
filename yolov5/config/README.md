# Gym Detection Configuration

This folder contains configuration for the Edge ChangeDetection integration.

## Configuration sources

1) YAML file: `yolov5/config/gym_detection.yaml`
2) Environment variables (override YAML values)

## Environment variables

- `GYM_HOST` (default: `https://mouseku.pythonanywhere.com`)
- `GYM_SECURITY_KEY` (required to authenticate)
- `GYM_MACHINE_ID` (default: `1`)
- `GYM_TARGET_CLASS` (default: `person`)

## Example

```bash
export GYM_HOST="https://mouseku.pythonanywhere.com"
export GYM_SECURITY_KEY="YOUR_KEY"
export GYM_MACHINE_ID="1"
export GYM_TARGET_CLASS="person"
```
