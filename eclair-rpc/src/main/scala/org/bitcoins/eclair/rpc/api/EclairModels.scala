package org.bitcoins.eclair.rpc.api

import java.util.UUID

import org.bitcoins.core.crypto.{
  DoubleSha256Digest,
  DoubleSha256DigestBE,
  ECDigitalSignature,
  Sha256Digest
}
import org.bitcoins.core.currency.Satoshis
import org.bitcoins.core.protocol.ln.channel.{ChannelState, FundedChannelId}
import org.bitcoins.core.protocol.ln.currency.MilliSatoshis
import org.bitcoins.core.protocol.ln.fee.FeeProportionalMillionths
import org.bitcoins.core.protocol.ln.node.NodeId
import org.bitcoins.core.protocol.ln.{
  LnHumanReadablePart,
  PaymentPreimage,
  ShortChannelId
}
import org.bitcoins.eclair.rpc.network.PeerState
import play.api.libs.json.JsObject

import scala.concurrent.duration.FiniteDuration

sealed abstract class EclairModels

case class GetInfoResult(
    nodeId: NodeId,
    alias: String,
    chainHash: DoubleSha256Digest,
    blockHeight: Long,
    publicAddresses: Seq[String])

case class PeerInfo(
    nodeId: NodeId,
    state: PeerState,
    address: Option[String],
    channels: Int)

/**
  * This is the data model returned by the RPC call
  * `channels nodeId`. The content of the objects
  * being returne differ based on whatever state
  * the channel is in. The member of this abstract
  * class are in eveyr channel state, whereas other
  * channel states may have extra information.
  */
sealed abstract class ChannelInfo {
  def nodeId: NodeId
  def channelId: FundedChannelId
  def localMsat: MilliSatoshis
  def remoteMsat: MilliSatoshis
  def state: ChannelState
}

/**
  * This represents the case where the
  * [[org.bitcoins.core.protocol.ln.channel.ChannelState ChannelState]] is
  * undetermined
  */
case class BaseChannelInfo(
    nodeId: NodeId,
    channelId: FundedChannelId,
    localMsat: MilliSatoshis,
    remoteMsat: MilliSatoshis,
    state: ChannelState
) extends ChannelInfo

/**
  * This represents the case where the channel is
  * in state `NORMAL` (i.e. an open channel)
  */
case class OpenChannelInfo(
    nodeId: NodeId,
    shortChannelId: ShortChannelId,
    channelId: FundedChannelId,
    localMsat: MilliSatoshis,
    remoteMsat: MilliSatoshis,
    state: ChannelState.NORMAL.type
) extends ChannelInfo

case class NodeInfo(
    signature: ECDigitalSignature,
    features: String,
    timestamp: Long,
    nodeId: NodeId,
    rgbColor: String,
    alias: String,
    addresses: Vector[String])

case class ChannelDesc(shortChannelId: ShortChannelId, a: NodeId, b: NodeId)

case class AuditResult(
    sent: Vector[SentPayment],
    relayed: Vector[RelayedPayment],
    received: Vector[ReceivedPayment]
)

case class NetworkFeesResult(
    remoteNodeId: NodeId,
    channelId: FundedChannelId,
    txId: DoubleSha256DigestBE,
    fee: Satoshis,
    txType: String,
    timestamp: FiniteDuration //milliseconds
)

case class ChannelStats(
    channelId: FundedChannelId,
    avgPaymentAmount: Satoshis,
    paymentCount: Long,
    relayFee: Satoshis,
    networkFee: Satoshis
)

case class UsableBalancesResult(
    remoteNodeId: NodeId,
    shortChannelId: ShortChannelId,
    canSend: MilliSatoshis,
    canReceive: MilliSatoshis,
    isPublic: Boolean
)

case class ReceivedPayment(
    paymentHash: Sha256Digest,
    parts: Vector[ReceivedPayment.Part]
)

object ReceivedPayment {
  case class Part(
      amount: MilliSatoshis,
      fromChannelId: FundedChannelId,
      timestamp: FiniteDuration //milliseconds
  )
}

case class RelayedPayment(
    amountIn: MilliSatoshis,
    amountOut: MilliSatoshis,
    paymentHash: Sha256Digest,
    fromChannelId: FundedChannelId,
    toChannelId: FundedChannelId,
    timestamp: FiniteDuration //milliseconds
)

case class SentPayment(
    id: PaymentId,
    paymentHash: Sha256Digest,
    paymentPreimage: PaymentPreimage,
    parts: Vector[SentPayment.Part]
)

object SentPayment {
  case class Part(
      id: PaymentId,
      amount: MilliSatoshis,
      feesPaid: MilliSatoshis,
      toChannelId: FundedChannelId,
      timestamp: FiniteDuration //milliseconds
  )
}

case class ChannelUpdate(
    signature: ECDigitalSignature,
    chainHash: DoubleSha256Digest,
    shortChannelId: ShortChannelId,
    timestamp: Long, //seconds
    messageFlags: Int,
    channelFlags: Int,
    cltvExpiryDelta: Int,
    htlcMinimumMsat: MilliSatoshis,
    feeProportionalMillionths: FeeProportionalMillionths,
    htlcMaximumMsat: Option[MilliSatoshis],
    feeBaseMsat: MilliSatoshis)

case class ChannelResult(
    nodeId: NodeId,
    channelId: FundedChannelId,
    state: ChannelState,
    feeBaseMsat: Option[MilliSatoshis],
    feeProportionalMillionths: Option[FeeProportionalMillionths],
    data: JsObject) {
  import org.bitcoins.eclair.rpc.client.JsonReaders._
  lazy val shortChannelId: Option[ShortChannelId] =
    (data \ "shortChannelId").validate[ShortChannelId].asOpt
}

// ChannelResult ends here

case class InvoiceResult(
    prefix: LnHumanReadablePart,
    timestamp: FiniteDuration, //seconds
    nodeId: NodeId,
    serialized: String,
    description: String,
    paymentHash: Sha256Digest,
    expiry: FiniteDuration)

case class PaymentId(value: UUID) {
  override def toString: String = value.toString
}

case class PaymentRequest(
    prefix: LnHumanReadablePart,
    timestamp: FiniteDuration, //seconds
    nodeId: NodeId,
    serialized: String,
    description: String,
    paymentHash: Sha256Digest,
    expiry: FiniteDuration, //seconds
    amount: Option[MilliSatoshis])

case class OutgoingPayment(
    id: PaymentId,
    parentId: PaymentId,
    externalId: Option[String],
    paymentHash: Sha256Digest,
    amount: MilliSatoshis,
    targetNodeId: NodeId,
    createdAt: FiniteDuration, //milliseconds
    paymentRequest: Option[PaymentRequest],
    status: OutgoingPaymentStatus)

case class IncomingPayment(
    paymentRequest: PaymentRequest,
    paymentPreimage: PaymentPreimage,
    createdAt: FiniteDuration, //milliseconds
    status: IncomingPaymentStatus)

sealed trait IncomingPaymentStatus

object IncomingPaymentStatus {

  case object Pending extends IncomingPaymentStatus

  case object Expired extends IncomingPaymentStatus

  case class Received(amount: MilliSatoshis, receivedAt: Long //milliseconds
  ) extends IncomingPaymentStatus

}

sealed trait OutgoingPaymentStatus

object OutgoingPaymentStatus {
  case object Pending extends OutgoingPaymentStatus

  case class Succeeded(
      paymentPreimage: PaymentPreimage,
      feesPaid: MilliSatoshis,
      route: Seq[Hop],
      completedAt: FiniteDuration //milliseconds
  ) extends OutgoingPaymentStatus

  case class Failed(failures: Seq[PaymentFailure]) extends OutgoingPaymentStatus
}

case class PaymentFailure(
    failureType: PaymentFailure.Type,
    failureMessage: String,
    failedRoute: Seq[Hop])

object PaymentFailure {
  sealed trait Type
  case object Local extends Type
  case object Remote extends Type
  case object UnreadableRemote extends Type
}
case class Hop(
    nodeId: NodeId,
    nextNodeId: NodeId,
    shortChannelId: Option[ShortChannelId])

sealed trait WebSocketEvent

object WebSocketEvent {

  case class PaymentRelayed(
      amountIn: MilliSatoshis,
      amountOut: MilliSatoshis,
      paymentHash: Sha256Digest,
      fromChannelId: FundedChannelId,
      toChannelId: FundedChannelId,
      timestamp: FiniteDuration //milliseconds
  ) extends WebSocketEvent

  case class PaymentReceived(
      paymentHash: Sha256Digest,
      parts: Vector[PaymentReceived.Part]
  ) extends WebSocketEvent

  object PaymentReceived {
    case class Part(
        amount: MilliSatoshis,
        fromChannelId: FundedChannelId,
        timestamp: FiniteDuration // milliseconds
    )
  }
  case class PaymentFailed(
      id: PaymentId,
      paymentHash: Sha256Digest,
      failures: Vector[String],
      timestamp: FiniteDuration // milliseconds
  ) extends WebSocketEvent

  case class PaymentSent(
      id: PaymentId,
      paymentHash: Sha256Digest,
      paymentPreimage: PaymentPreimage,
      parts: Vector[PaymentSent.Part]
  ) extends WebSocketEvent

  object PaymentSent {
    case class Part(
        id: PaymentId,
        amount: MilliSatoshis,
        feesPaid: MilliSatoshis,
        toChannelId: FundedChannelId,
        timestamp: FiniteDuration // milliseconds
    )
  }

  case class PaymentSettlingOnchain(
      amount: MilliSatoshis,
      paymentHash: Sha256Digest,
      timestamp: FiniteDuration //milliseconds
  ) extends WebSocketEvent

}
