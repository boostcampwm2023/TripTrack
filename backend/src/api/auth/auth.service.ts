import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { UserService } from '@/domain/user/user.service';
import * as bcrypt from 'bcrypt';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { RefreshTokenService } from '@/domain/refresh-token/refresh-token.service';
import { LoginAuthDto } from './dto/login-auth.dto';
import { LoginDto } from './dto/login.dto';
import { CreateAuthDto } from './dto/create-auth.dto';
import { SignUpDto } from './dto/signup.dto';

@Injectable()
export class AuthService {
  constructor(
    readonly userService: UserService,
    readonly jwtService: JwtService,
    readonly configService: ConfigService,
    readonly refreshTokenService: RefreshTokenService,
  ) {}

  async signup(createAuthDto: CreateAuthDto) {
    const user = await this.userService.findUserByUniqueInput({ email: createAuthDto.email });

    if (user) {
      throw new ConflictException('이미 존재하는 이메일입니다.');
    }

    const newUser = await this.userService.create(createAuthDto);

    return SignUpDto.of(newUser);
  }

  async validateUser(loginAuthDto: LoginAuthDto) {
    const user = await this.userService.findUserByUniqueInput({ email: loginAuthDto.email });

    if (!user) {
      throw new NotFoundException('해당 유저가 존재하지 않습니다.');
    }

    await this.verifyPassword(loginAuthDto.password, user.password);
    const accessToken = await this.refreshTokenService.generateAccessToken(user);
    const refreshToken = await this.refreshTokenService.generateRefreshToken(user);
    await this.setCurrentRefreshToken(refreshToken, user.uuid);

    return LoginDto.of(accessToken, refreshToken);
  }

  async verifyPassword(plainText: string, hash: string) {
    const isPasswordMatching = await bcrypt.compare(plainText, hash);
    if (!isPasswordMatching) {
      throw new BadRequestException('잘못된 비밀번호입니다.');
    }
  }

  async logout(refreshToken: string) {
    const decodedRefreshToken = await this.jwtService.verifyAsync(refreshToken, {
      secret: this.configService.getOrThrow<string>('JWT_REFRESH_SECRET'),
    });

    const user = await this.userService.findUserByUniqueInput({ uuid: decodedRefreshToken.uuid });

    if (!user) {
      throw new NotFoundException('해당 유저가 존재하지 않습니다.');
    }

    await this.refreshTokenService.delete({ userUuid: user.uuid });
  }

  async setCurrentRefreshToken(refreshToken: string, userUuid: string) {
    const isRefreshToken = await this.refreshTokenService.findRefreshTokenByUnique({ userUuid: userUuid });

    if (!isRefreshToken) {
      const createdRefreshToken = await this.refreshTokenService.create({
        userUuid: userUuid,
        token: refreshToken,
        expiresAt: await this.refreshTokenService.getCurrentRefreshTokenExp(),
      });

      return createdRefreshToken;
    }

    const updatedRefreshToken = this.refreshTokenService.update({
      userUuid: userUuid,
      token: refreshToken,
      expiresAt: await this.refreshTokenService.getCurrentRefreshTokenExp(),
    });

    return updatedRefreshToken;
  }
}
