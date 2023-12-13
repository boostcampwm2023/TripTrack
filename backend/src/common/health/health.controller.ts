import { Controller, Get } from '@nestjs/common';
import {
  HealthCheckService,
  HttpHealthIndicator,
  HealthCheck,
  PrismaHealthIndicator,
  MicroserviceHealthIndicator,
} from '@nestjs/terminus';
import { NoAuth } from '@/common/decorator/no-auth.decorator';
import { PrismaProvider } from '@/common/prisma/prisma.provider';
import { Transport } from '@nestjs/microservices';
import { ConfigService } from '@nestjs/config';

@Controller('health')
export class HealthController {
  constructor(
    private health: HealthCheckService,
    private http: HttpHealthIndicator,
    private prisma: PrismaHealthIndicator,
    private prismaProvider: PrismaProvider,
    private microService: MicroserviceHealthIndicator,
    private configService: ConfigService,
  ) {}

  @Get()
  @NoAuth()
  @HealthCheck()
  check() {
    return this.health.check([
      () => this.http.pingCheck('HTTP', 'https://docs.nestjs.com'),
      () => this.prisma.pingCheck('DATABASE', this.prismaProvider.get()),
      () =>
        this.microService.pingCheck('DATA_MICROSERVICE', {
          transport: Transport.RMQ,
          options: {
            urls: [this.configService.getOrThrow<string>('RMQ_HOST')],
            queue: this.configService.getOrThrow<string>('RMQ_QUEUE'),
            queueOptions: { durable: true },
          },
        }),
      () =>
        this.microService.pingCheck('MEDIA_MICROSERVICE', {
          transport: Transport.RMQ,
          options: {
            urls: [this.configService.getOrThrow<string>('RMQ_HOST')],
            queue: this.configService.getOrThrow<string>('RMQ_MEDIA_QUEUE'),
            queueOptions: { durable: true },
          },
        }),
    ]);
  }
}
