# ALCI Assessor

An AI Literacy assessment chatbot built with the [Embabel Agent Framework](https://github.com/embabel/embabel-agent).

Conducts the AI Literacy Collaboration Index (ALCI) assessment through
a conversational interface — CLI or web chat. Scans repositories for
observable evidence, administers the ALCI instrument, assesses literacy
level across three disciplines, and generates recommendations.

## Quick Start

```bash
# CLI mode
docker run -it \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  alci-assessor

# Web mode
docker run -p 8080:8080 \
  -v /path/to/project:/repo \
  -v ./assessments:/assessments \
  -e ANTHROPIC_API_KEY=your-key \
  -e SPRING_PROFILES_ACTIVE=web \
  alci-assessor
```

## License

[Apache License 2.0](LICENSE)
