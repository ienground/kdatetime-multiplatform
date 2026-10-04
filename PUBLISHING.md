# Maven Central 배포

Git에서 제외되는 `local.properties`에 개인 배포 그룹을 지정합니다.

```properties
publication.group=zone.ien
publication.publishingType=automatic
```

Central Portal 토큰과 서명 설정은 `~/.gradle/gradle.properties`의
`mavenCentralUsername`, `mavenCentralPassword`, `signing.keyId`,
`signing.password`, `signing.secretKeyRingFile`을 사용합니다.
프로젝트별로 지정하려면 `local.properties`에 `ossrhUsername`, `ossrhPassword`와
동일한 `signing.*` 항목을 설정하면 됩니다. 기존 환경 변수 설정도 지원합니다.

```bash
./gradlew publishToMavenCentral
```

전체 플랫폼 아티팩트를 서명·업로드한 뒤 배포 그룹의 네임스페이스로 Central Portal에
전송합니다. `automatic`은 검증 통과 후 자동으로 공개합니다.
설정을 생략하거나 `user_managed`로 지정하면 Portal에서 직접 공개를 승인합니다.
사용하는 토큰에는 해당 네임스페이스의 배포 권한이 있어야 합니다.

배포 그룹이 등록된 네임스페이스의 하위 그룹이면 `local.properties`에
`publication.namespace`로 등록된 네임스페이스를 별도로 지정합니다.

기존 `publishAllPublicationsToSonatypeRepository`는 아티팩트 업로드만 수행합니다.
