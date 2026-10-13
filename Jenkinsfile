pipeline {
  agent any
  environment {
    VAULT_ADDR = 'http://vault:8200'
    APP        = 'secret-locker'
  }
  stages {
    stage('Build image') {
      steps {
        sh 'docker build -t $APP:$BUILD_NUMBER .'
      }
    }

    stage('Issue credentials & deploy') {
      steps {
        withCredentials([
          string(credentialsId: 'vault-jenkins-role-id',   variable: 'J_ROLE_ID'),
          string(credentialsId: 'vault-jenkins-secret-id', variable: 'J_SECRET_ID')
        ]) {
          sh '''
            set -eu
            # 1) Jenkins 자신의 AppRole로 로그인
            J_TOKEN=$(jq -n --arg r "$J_ROLE_ID" --arg s "$J_SECRET_ID" '{role_id:$r, secret_id:$s}' \
              | curl -sf -X POST -d @- "$VAULT_ADDR/v1/auth/approle/login" \
              | jq -r .auth.client_token)

            # 2) 앱 역할의 role-id 조회
            ROLE_ID=$(curl -sf -H "X-Vault-Token: $J_TOKEN" \
              "$VAULT_ADDR/v1/auth/approle/role/$APP/role-id" | jq -r .data.role_id)

            # 3) 앱 역할의 secret-id를 래핑해서 발급 (래핑 토큰만 받는다)
            WRAP_TOKEN=$(curl -sf -X POST -H "X-Vault-Token: $J_TOKEN" -H "X-Vault-Wrap-TTL: 120s" \
              "$VAULT_ADDR/v1/auth/approle/role/$APP/secret-id" | jq -r .wrap_info.token)

            # 4) 앱 배포
            docker rm -f "$APP" >/dev/null 2>&1 || true
            docker run -d --name "$APP" --network lab-net -p 8081:8080 \
              -v secret-locker-data:/data \
              -e VAULT_ADDR="$VAULT_ADDR" \
              -e VAULT_ROLE_ID="$ROLE_ID" \
              -e VAULT_WRAPPED_SECRET_ID="$WRAP_TOKEN" \
              -e LOCKER_CRYPTO_MODE="$CRYPTO_MODE" \
              "$APP:$BUILD_NUMBER"

            # 5) Jenkins 토큰 폐기
            curl -sf -X POST -H "X-Vault-Token: $J_TOKEN" "$VAULT_ADDR/v1/auth/token/revoke-self" || true
          '''
        }
      }
    }

    stage('Smoke test') {
      steps {
        sh '''
          set -eu
          for i in $(seq 1 30); do
            curl -sf "http://$APP:8080/actuator/health" >/dev/null && break
            sleep 2
          done
          ID=$(curl -sf -X POST -H 'Content-Type: application/json' \
            -d '{"owner":"SMOKE","label":"build-'"$BUILD_NUMBER"'","value":"110-123-456789"}' \
            "http://$APP:8080/api/records" | jq -r .id)
          VALUE=$(curl -sf "http://$APP:8080/api/records/$ID" | jq -r .value)
          test "$VALUE" = "110-123-456789"
          # 데이터가 볼륨에 남으므로 스모크 레코드는 지운다
          curl -sf -X DELETE "http://$APP:8080/api/records/$ID"
          curl -sf "http://$APP:8080/api/crypto/status"
        '''
      }
    }
  }
  post {
    failure {
      sh 'docker logs --tail 80 $APP || true'
    }
  }
}
